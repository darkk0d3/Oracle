package com.oracle.mrt3.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.oracle.mrt3.data.model.ActiveTripState
import com.oracle.mrt3.data.model.EmergencyReport
import com.oracle.mrt3.data.model.MRT3_STATIONS
import com.oracle.mrt3.data.model.PersonalContact
import com.oracle.mrt3.data.model.TripHistoryItem
import com.oracle.mrt3.data.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val uid get() = auth.currentUser?.uid ?: ""

    // ── User Profile ──────────────────────────────────────────────────────────

    suspend fun getProfile(): UserProfile? {
        if (uid.isEmpty()) return null
        return try {
            val snap = db.collection("users").document(uid)
                .collection("profile").document("data").get().await()
            snap.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateProfile(displayName: String) {
        if (uid.isEmpty()) return
        db.collection("users").document(uid)
            .collection("profile").document("data")
            .set(mapOf("displayName" to displayName, "updatedAt" to Timestamp.now()))
            .await()
    }

    // ── Trip History ──────────────────────────────────────────────────────────

    suspend fun saveTripHistory(
        origin: String,
        destination: String,
        fare: Double,
        isDiscounted: Boolean
    ) {
        if (uid.isEmpty()) return
        db.collection("users").document(uid)
            .collection("tripHistory")
            .add(
                mapOf(
                    "origin"       to origin,
                    "destination"  to destination,
                    "fare"         to fare,
                    "isDiscounted" to isDiscounted,
                    "createdAt"    to Timestamp.now()
                )
            ).await()
    }

    fun getTripHistory(): Flow<List<TripHistoryItem>> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener: ListenerRegistration = db.collection("users").document(uid)
            .collection("tripHistory")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    TripHistoryItem(
                        id           = doc.id,
                        origin       = doc.getString("origin") ?: "",
                        destination  = doc.getString("destination") ?: "",
                        fare         = doc.getDouble("fare") ?: 0.0,
                        isDiscounted = doc.getBoolean("isDiscounted") ?: false,
                        createdAt    = doc.getTimestamp("createdAt")
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Personal Contacts ─────────────────────────────────────────────────────

    fun getContacts(): Flow<List<PersonalContact>> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(uid)
            .collection("contacts")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    PersonalContact(
                        id    = doc.id,
                        name  = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: ""
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addContact(name: String, phone: String) {
        if (uid.isEmpty()) return
        db.collection("users").document(uid)
            .collection("contacts")
            .add(mapOf("name" to name, "phone" to phone))
            .await()
    }

    suspend fun deleteContact(contactId: String) {
        if (uid.isEmpty()) return
        db.collection("users").document(uid)
            .collection("contacts").document(contactId)
            .delete().await()
    }

    // ── Emergency Reports ─────────────────────────────────────────────────────

    fun getEmergencyReports(): Flow<List<EmergencyReport>> = callbackFlow {
        val listener = db.collection("emergencyReports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    EmergencyReport(
                        id          = doc.id,
                        type        = doc.getString("type") ?: "",
                        station     = doc.getString("station") ?: "",
                        description = doc.getString("description") ?: "",
                        hasPhoto    = doc.getBoolean("hasPhoto") ?: false,
                        userId      = doc.getString("userId") ?: "",
                        status      = doc.getString("status") ?: "pending",
                        createdAt   = doc.getTimestamp("createdAt")
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun submitEmergencyReport(
        type: String,
        station: String,
        description: String,
        hasPhoto: Boolean
    ) {
        db.collection("emergencyReports")
            .add(
                mapOf(
                    "type"        to type,
                    "station"     to station,
                    "description" to description,
                    "hasPhoto"    to hasPhoto,
                    "userId"      to uid,
                    "status"      to "pending",
                    "createdAt"   to Timestamp.now()
                )
            ).await()
    }

    // ── Station Statuses ──────────────────────────────────────────────────────

    fun getStationStatuses(): Flow<Map<String, String>> = callbackFlow {
        val listeners = mutableListOf<ListenerRegistration>()
        val statusMap = mutableMapOf<String, String>()

        MRT3_STATIONS.forEach { station ->
            val l = db.collection("stationStatuses").document(station.name)
                .addSnapshotListener { snap, _ ->
                    statusMap[station.name] = snap?.getString("status") ?: "clear"
                    trySend(statusMap.toMap())
                }
            listeners.add(l)
        }
        awaitClose { listeners.forEach { it.remove() } }
    }

    // ── Active Trip State ─────────────────────────────────────────────────────

    suspend fun saveActiveTripState(origin: String, destination: String, startedAt: Long) {
        if (uid.isEmpty()) return
        db.collection("users").document(uid)
            .collection("activeTripState").document("current")
            .set(mapOf(
                "origin"         to origin,
                "destination"    to destination,
                "startedAt"      to Timestamp(startedAt / 1000, ((startedAt % 1000) * 1_000_000).toInt()),
                "currentStation" to null,
                "isActive"       to true
            )).await()
    }

    suspend fun clearActiveTripState() {
        if (uid.isEmpty()) return
        db.collection("users").document(uid)
            .collection("activeTripState").document("current")
            .set(mapOf("isActive" to false)).await()
    }

    suspend fun updateCurrentStation(stationName: String?) {
        if (uid.isEmpty()) return
        db.collection("users").document(uid)
            .collection("activeTripState").document("current")
            .set(mapOf("currentStation" to stationName), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    fun getActiveTripState(): Flow<ActiveTripState?> = callbackFlow {
        if (uid.isEmpty()) { trySend(null); close(); return@callbackFlow }
        val listener = db.collection("users").document(uid)
            .collection("activeTripState").document("current")
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) { trySend(null); return@addSnapshotListener }
                trySend(ActiveTripState(
                    origin         = snap.getString("origin") ?: "",
                    destination    = snap.getString("destination") ?: "",
                    startedAt      = snap.getTimestamp("startedAt"),
                    currentStation = snap.getString("currentStation"),
                    isActive       = snap.getBoolean("isActive") ?: false
                ))
            }
        awaitClose { listener.remove() }
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    suspend fun submitFeedback(rating: Int, comment: String) {
        db.collection("feedback")
            .add(
                mapOf(
                    "userId"    to uid,
                    "rating"    to rating,
                    "comment"   to comment,
                    "createdAt" to Timestamp.now()
                )
            ).await()
    }
}
