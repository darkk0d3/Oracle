package com.oracle.mrt3.data.model

import com.google.firebase.Timestamp

// ── Station ───────────────────────────────────────────────────────────────────
data class Station(
    val name: String,
    val lat: Double,
    val lng: Double,
    val travelToNext: Int,  // seconds to next station
    val baseDwell: Int      // seconds dwell time
)

val MRT3_STATIONS = listOf(
    Station("North Avenue",         14.6534, 121.0335, 120, 30),
    Station("Quezon Avenue",        14.6425, 121.0392, 110, 30),
    Station("GMA Kamuning",         14.6350, 121.0435, 115, 30),
    Station("Araneta Center-Cubao", 14.6194, 121.0563, 120, 45),
    Station("Santolan-Annapolis",   14.6078, 121.0565, 130, 30),
    Station("Ortigas",              14.5878, 121.0567, 120, 45),
    Station("Shaw Boulevard",       14.5813, 121.0536, 110, 30),
    Station("Boni",                 14.5739, 121.0482, 115, 30),
    Station("Guadalupe",            14.5673, 121.0454, 120, 30),
    Station("Buendia",              14.5541, 121.0338, 115, 30),
    Station("Ayala",                14.5491, 121.0281, 130, 45),
    Station("Magallanes",           14.5348, 121.0195, 110, 30),
    Station("Taft Avenue",          14.5376, 121.0020,   0,  0)
)

val STATION_NAMES: List<String> = MRT3_STATIONS.map { it.name }

// ── Station Status ────────────────────────────────────────────────────────────
enum class StationStatus(val label: String, val multiplier: Double) {
    CLEAR("Clear", 1.0),
    SLIGHT("Slight Delay", 1.5),
    HEAVY("Heavy Delay", 3.0),
    OFFLINE("Offline", 0.0)
}

fun stationStatusFromString(s: String): StationStatus = when (s.lowercase()) {
    "slight"  -> StationStatus.SLIGHT
    "heavy"   -> StationStatus.HEAVY
    "offline" -> StationStatus.OFFLINE
    else      -> StationStatus.CLEAR
}

// ── Firestore data classes ────────────────────────────────────────────────────
data class TripHistoryItem(
    val id: String = "",
    val origin: String = "",
    val destination: String = "",
    val fare: Double = 0.0,
    val isDiscounted: Boolean = false,
    val createdAt: Timestamp? = null
)

data class EmergencyReport(
    val id: String = "",
    val type: String = "",
    val station: String = "",
    val description: String = "",
    val hasPhoto: Boolean = false,
    val userId: String = "",
    val status: String = "pending",
    val createdAt: Timestamp? = null
)

data class PersonalContact(
    val id: String = "",
    val name: String = "",
    val phone: String = ""
)

data class UserProfile(
    val displayName: String = "",
    val updatedAt: Timestamp? = null
)

// ── Picker options ────────────────────────────────────────────────────────────
val EMERGENCY_TYPES = listOf(
    "Medical Emergencies",
    "Fire and Hazardous Situations",
    "Security Threats",
    "Technical/Operational Accidents",
    "Natural Disasters",
    "Crowd-Related Incidents",
    "Other Emergencies"
)

// Pair of label -> isDiscounted
val PASSENGER_TYPES = listOf(
    "2026 Special Discount" to true
)

// ── Active Trip State (Firestore: users/{uid}/activeTripState/current) ─────────
data class ActiveTripState(
    val origin: String = "",
    val destination: String = "",
    val startedAt: Timestamp? = null,
    val currentStation: String? = null,
    val isActive: Boolean = false
)

// ── Station label abbreviations for the tracker strip ─────────────────────────
val STATION_ABBREV: Map<String, String> = mapOf(
    "North Avenue"         to "NAvE",
    "Quezon Avenue"        to "QAve",
    "GMA Kamuning"         to "GMA",
    "Araneta Center-Cubao" to "Cubao",
    "Santolan-Annapolis"   to "Sntln",
    "Ortigas"              to "Ortgs",
    "Shaw Boulevard"       to "Shaw",
    "Boni"                 to "Boni",
    "Guadalupe"            to "Guad",
    "Buendia"              to "Buend",
    "Ayala"                to "Ayala",
    "Magallanes"           to "Mgln",
    "Taft Avenue"          to "Taft"
)
