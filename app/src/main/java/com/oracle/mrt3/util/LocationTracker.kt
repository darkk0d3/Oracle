package com.oracle.mrt3.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.locationDataStore: DataStore<Preferences> by preferencesDataStore("location_cache")

@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedClient: FusedLocationProviderClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val KEY_LAT = doublePreferencesKey("last_lat")
        private val KEY_LNG = doublePreferencesKey("last_lng")
    }

    @SuppressLint("MissingPermission")
    val locationFlow: Flow<Location?> = callbackFlow {
        // Emit persisted last-known location immediately so the UI is never blank
        try {
            val prefs = context.locationDataStore.data.first()
            val lat = prefs[KEY_LAT]
            val lng = prefs[KEY_LNG]
            if (lat != null && lng != null) {
                trySend(Location("cache").apply {
                    latitude = lat
                    longitude = lng
                })
            }
        } catch (_: Exception) {}

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(3_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                trySend(location)
                scope.launch {
                    try {
                        context.locationDataStore.edit { prefs ->
                            prefs[KEY_LAT] = location.latitude
                            prefs[KEY_LNG] = location.longitude
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }
        .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)
}
