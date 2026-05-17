package com.oracle.mrt3.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.oracle.mrt3.data.model.MRT3_STATIONS
import com.oracle.mrt3.data.model.STATION_NAMES
import com.oracle.mrt3.data.model.StationStatus
import com.oracle.mrt3.data.model.stationStatusFromString
import com.oracle.mrt3.data.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Holds GPS-inferred live train position, ETA from current segment, and signal state.
 * Requires: com.google.android.gms:play-services-location in build.gradle
 */
@HiltViewModel
class LiveTrainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FirestoreRepository
) : ViewModel() {

    // lat/lng pair — kept as primitives to avoid any OSMDroid dependency in the VM layer
    data class TrainPosition(val latitude: Double, val longitude: Double)

    data class TrainState(
        val inferredStationIndex: Int = -1,
        val inferredPosition: TrainPosition? = null,
        val signalLost: Boolean = false,
        val locating: Boolean = false,
        val etaMinutes: Int = -1,
        val remainingStops: Int = 0
    )

    private val _trainState = MutableStateFlow(TrainState())
    val trainState: StateFlow<TrainState> = _trainState.asStateFlow()

    private val _stationStatuses = MutableStateFlow<Map<String, String>>(emptyMap())

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private var trackingJob: Job? = null
    private var currentDestination: String = ""

    init {
        observeStatuses()
    }

    fun startTracking(destination: String) {
        currentDestination = destination
        if (trackingJob?.isActive == true) return
        _trainState.value = TrainState(locating = true)
        trackingJob = viewModelScope.launch {
            locationUpdatesFlow().collect { loc ->
                if (loc != null) processLocation(loc.latitude, loc.longitude)
                else _trainState.value = TrainState(signalLost = true, locating = false)
            }
        }
    }

    fun updateDestination(destination: String) {
        currentDestination = destination
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _trainState.value = TrainState()
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdatesFlow(): Flow<Location?> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setMaxUpdateDelayMillis(15_000L)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                trySend(result.lastLocation)
            }
            override fun onLocationAvailability(avail: LocationAvailability) {
                if (!avail.isLocationAvailable) trySend(null)
            }
        }
        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            trySend(null)
            close()
        }
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    private fun processLocation(lat: Double, lng: Double) {
        val minCorridorDist = minDistanceToCorridorMeters(lat, lng)
        if (minCorridorDist > 200.0) {
            _trainState.value = TrainState(signalLost = true, locating = false)
            return
        }

        val projectedPos = projectOntoCorridorPoint(lat, lng)
        val nearestEntry = MRT3_STATIONS.mapIndexed { i, s ->
            i to haversineMeters(lat, lng, s.lat, s.lng)
        }.minByOrNull { it.second }

        val nearestIdx = nearestEntry?.first ?: -1
        val destIdx    = STATION_NAMES.indexOf(currentDestination)
        val remaining  = if (destIdx >= 0 && nearestIdx >= 0) abs(destIdx - nearestIdx) else 0
        val eta        = if (nearestIdx >= 0 && destIdx >= 0) calculateEtaFromIdx(nearestIdx, destIdx) else -1

        _trainState.value = TrainState(
            inferredStationIndex = nearestIdx,
            inferredPosition     = TrainPosition(projectedPos.first, projectedPos.second),
            signalLost           = false,
            locating             = false,
            etaMinutes           = eta,
            remainingStops       = remaining
        )

        // Persist inferred station to Firestore so ProfileScreen can show it offline
        if (nearestIdx >= 0) viewModelScope.launch {
            runCatching { repository.updateCurrentStation(MRT3_STATIONS[nearestIdx].name) }
        }
    }

    private fun calculateEtaFromIdx(fromIdx: Int, toIdx: Int): Int {
        if (fromIdx == toIdx) return 0
        val statuses = _stationStatuses.value
        val range = if (fromIdx < toIdx) fromIdx until toIdx else toIdx until fromIdx
        var total = 0
        for (i in range) {
            val st     = MRT3_STATIONS[i]
            val status = stationStatusFromString(statuses[st.name] ?: "clear")
            if (status == StationStatus.OFFLINE) return -1
            total += (st.travelToNext * status.multiplier).toInt() + st.baseDwell
        }
        return total / 60
    }

    private fun observeStatuses() = viewModelScope.launch {
        repository.getStationStatuses().collect { _stationStatuses.value = it }
    }

    // ── Geo helpers ───────────────────────────────────────────────────────────

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R    = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a    = sin(dLat / 2).pow(2.0) +
                   cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        return 2 * R * asin(sqrt(a))
    }

    private fun minDistanceToCorridorMeters(lat: Double, lng: Double): Double =
        MRT3_STATIONS.zipWithNext().minOf { (a, b) ->
            distToSegmentMeters(lat, lng, a.lat, a.lng, b.lat, b.lng)
        }

    private fun distToSegmentMeters(
        pLat: Double, pLng: Double,
        aLat: Double, aLng: Double,
        bLat: Double, bLng: Double
    ): Double {
        val dx    = bLat - aLat; val dy = bLng - aLng
        val lenSq = dx * dx + dy * dy
        val (nLat, nLng) = if (lenSq == 0.0) aLat to aLng else {
            val t  = ((pLat - aLat) * dx + (pLng - aLng) * dy) / lenSq
            val tc = t.coerceIn(0.0, 1.0)
            (aLat + tc * dx) to (aLng + tc * dy)
        }
        return haversineMeters(pLat, pLng, nLat, nLng)
    }

    private fun projectOntoCorridorPoint(lat: Double, lng: Double): Pair<Double, Double> {
        var minDist  = Double.MAX_VALUE
        var best     = lat to lng
        MRT3_STATIONS.zipWithNext().forEach { (a, b) ->
            val dx    = b.lat - a.lat; val dy = b.lng - a.lng
            val lenSq = dx * dx + dy * dy
            val (nLat, nLng) = if (lenSq == 0.0) a.lat to a.lng else {
                val t  = ((lat - a.lat) * dx + (lng - a.lng) * dy) / lenSq
                val tc = t.coerceIn(0.0, 1.0)
                (a.lat + tc * dx) to (a.lng + tc * dy)
            }
            val d = haversineMeters(lat, lng, nLat, nLng)
            if (d < minDist) { minDist = d; best = nLat to nLng }
        }
        return best
    }
}
