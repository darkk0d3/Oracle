package com.oracle.mrt3.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oracle.mrt3.data.model.MRT3_STATIONS
import com.oracle.mrt3.data.model.STATION_NAMES
import com.oracle.mrt3.data.model.StationStatus
import com.oracle.mrt3.data.model.TripHistoryItem
import com.oracle.mrt3.data.model.stationStatusFromString
import com.oracle.mrt3.data.repository.FirestoreRepository
import com.oracle.mrt3.util.ConnectivityObserver
import com.oracle.mrt3.util.LocationTracker
import com.oracle.mrt3.util.OfflineModeHelper
import com.oracle.mrt3.util.TripProgressManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TripState(
    val origin: String = "",
    val destination: String = "",
    val fare: Double = 0.0,
    val isDiscounted: Boolean = true,
    val startedAt: Long? = null
)

sealed class SaveState {
    object Idle    : SaveState()
    object Loading : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}

@HiltViewModel
class TripViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val locationTracker: LocationTracker,
    private val tripProgressManager: TripProgressManager
) : ViewModel() {

    private val _tripState = MutableStateFlow(TripState())
    val tripState: StateFlow<TripState> = _tripState.asStateFlow()

    private val _tripHistory = MutableStateFlow<List<TripHistoryItem>>(emptyList())
    val tripHistory: StateFlow<List<TripHistoryItem>> = _tripHistory.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _stationStatuses = MutableStateFlow<Map<String, String>>(emptyMap())
    val stationStatuses: StateFlow<Map<String, String>> = _stationStatuses.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val userLocation: StateFlow<Location?> = locationTracker.locationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentStationEstimate: StateFlow<String> = tripProgressManager.estimatedStation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val elapsedMinutes: StateFlow<Int> = tripProgressManager.elapsedSeconds
        .map { (it / 60).toInt() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val remainingMinutes: StateFlow<Int> = combine(
        _tripState,
        _stationStatuses,
        tripProgressManager.elapsedSeconds
    ) { trip, statuses, elapsed ->
        if (trip.origin.isEmpty() || trip.destination.isEmpty()) return@combine 0
        val totalEta = calculateEta(trip.origin, trip.destination, statuses)
        if (totalEta < 0) 0 else maxOf(0, totalEta - (elapsed / 60).toInt())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        loadTripHistory()
        observeStationStatuses()
        restoreActiveTripState()
    }

    fun setOrigin(station: String) {
        _tripState.update { it.copy(origin = station) }
        recalcFare()
    }

    fun setDestination(station: String) {
        _tripState.update { it.copy(destination = station) }
        recalcFare()
    }

    fun setPassengerType(isDiscounted: Boolean) {
        _tripState.update { it.copy(isDiscounted = isDiscounted) }
        recalcFare()
    }

    private fun recalcFare() {
        val s = _tripState.value
        if (s.origin.isEmpty() || s.destination.isEmpty()) return
        if (s.origin == s.destination) return
        val discountedFare = OfflineModeHelper.getFareOffline(s.origin, s.destination)
        if (discountedFare == 0.0) return
        val fare = if (s.isDiscounted) discountedFare else discountedFare * 2
        _tripState.update { it.copy(fare = fare) }
    }

    fun saveTrip() {
        val s = _tripState.value
        if (s.origin.isEmpty() || s.destination.isEmpty()) return
        _saveState.value = SaveState.Loading
        viewModelScope.launch {
            try {
                repository.saveTripHistory(s.origin, s.destination, s.fare, s.isDiscounted)
                val startedAt = System.currentTimeMillis()
                _tripState.update { it.copy(startedAt = startedAt) }
                tripProgressManager.startTrip(s.origin, s.destination, startedAt)
                try { repository.saveActiveTripState(s.origin, s.destination, startedAt) } catch (_: Exception) {}
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Failed to save trip")
            }
        }
    }

    fun endTrip() {
        _tripState.update { it.copy(startedAt = null) }
        tripProgressManager.stopTrip()
        viewModelScope.launch {
            try { repository.clearActiveTripState() } catch (_: Exception) {}
        }
    }

    fun resetSaveState() { _saveState.value = SaveState.Idle }

    fun calculateEta(origin: String, destination: String, statuses: Map<String, String>): Int {
        val fromIdx = STATION_NAMES.indexOf(origin)
        val toIdx   = STATION_NAMES.indexOf(destination)
        if (fromIdx < 0 || toIdx < 0 || fromIdx == toIdx) return 0
        val range = if (fromIdx < toIdx) fromIdx until toIdx else toIdx until fromIdx
        var totalSeconds = 0
        for (i in range) {
            val st     = MRT3_STATIONS[i]
            val status = stationStatusFromString(statuses[st.name] ?: "clear")
            if (status == StationStatus.OFFLINE) return -1
            totalSeconds += (st.travelToNext * status.multiplier).toInt() + st.baseDwell
        }
        return totalSeconds / 60
    }

    fun getWorstStatus(origin: String, destination: String, statuses: Map<String, String>): StationStatus {
        val fromIdx = STATION_NAMES.indexOf(origin)
        val toIdx   = STATION_NAMES.indexOf(destination)
        if (fromIdx < 0 || toIdx < 0) return StationStatus.CLEAR
        val range = if (fromIdx < toIdx) fromIdx..toIdx else toIdx..fromIdx
        var worst = StationStatus.CLEAR
        for (i in range) {
            val s = stationStatusFromString(statuses[MRT3_STATIONS[i].name] ?: "clear")
            if (s.ordinal > worst.ordinal) worst = s
        }
        return worst
    }

    private fun loadTripHistory() = viewModelScope.launch {
        repository.getTripHistory().collect { _tripHistory.value = it }
    }

    private fun observeStationStatuses() = viewModelScope.launch {
        repository.getStationStatuses().collect { _stationStatuses.value = it }
    }

    private fun restoreActiveTripState() = viewModelScope.launch {
        try {
            val state = repository.getActiveTripState().first() ?: return@launch
            if (!state.isActive || state.origin.isEmpty() || state.destination.isEmpty()) return@launch
            val startedAt = state.startedAt?.toDate()?.time ?: return@launch
            _tripState.update {
                it.copy(
                    origin = state.origin,
                    destination = state.destination,
                    startedAt = startedAt
                )
            }
            recalcFare()
            tripProgressManager.startTrip(state.origin, state.destination, startedAt)
        } catch (_: Exception) {}
    }
}
