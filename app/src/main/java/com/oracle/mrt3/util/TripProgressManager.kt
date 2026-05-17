package com.oracle.mrt3.util

import com.oracle.mrt3.data.model.MRT3_STATIONS
import com.oracle.mrt3.data.model.STATION_NAMES
import com.oracle.mrt3.data.repository.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripProgressManager @Inject constructor(
    private val repository: FirestoreRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _estimatedStation = MutableStateFlow("")
    val estimatedStation: StateFlow<String> = _estimatedStation.asStateFlow()

    private var originIdx = -1
    private var destinationIdx = -1
    private var tripStartedAt = 0L
    private var lastReportedStation = ""

    fun startTrip(origin: String, destination: String, startedAt: Long) {
        stopTrip()
        originIdx = STATION_NAMES.indexOf(origin)
        destinationIdx = STATION_NAMES.indexOf(destination)
        tripStartedAt = startedAt
        lastReportedStation = origin
        _estimatedStation.value = origin

        tickerJob = scope.launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - tripStartedAt) / 1000L
                _elapsedSeconds.value = elapsed
                val station = deadReckonStation(elapsed)
                _estimatedStation.value = station
                if (station != lastReportedStation) {
                    lastReportedStation = station
                    try { repository.updateCurrentStation(station) } catch (_: Exception) {}
                }
                delay(1_000L)
            }
        }
    }

    fun stopTrip() {
        tickerJob?.cancel()
        tickerJob = null
        _elapsedSeconds.value = 0L
        _estimatedStation.value = ""
        originIdx = -1
        destinationIdx = -1
        tripStartedAt = 0L
        lastReportedStation = ""
    }

    private fun deadReckonStation(elapsedSeconds: Long): String {
        if (originIdx < 0 || destinationIdx < 0) return ""
        // Walk segments in travel direction; each station "owns" travelToNext + baseDwell seconds
        val segmentIndices = if (originIdx <= destinationIdx)
            (originIdx until destinationIdx).toList()
        else
            (originIdx downTo destinationIdx + 1).toList()

        var remaining = elapsedSeconds
        for (i in segmentIndices) {
            val station = MRT3_STATIONS[i]
            val segmentTime = (station.travelToNext + station.baseDwell).toLong()
            if (remaining < segmentTime) return station.name
            remaining -= segmentTime
        }
        return MRT3_STATIONS[destinationIdx].name
    }
}
