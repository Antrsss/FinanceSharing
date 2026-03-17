package com.example.financesharing.data.repository

import com.example.financesharing.data.firebase.FirebaseProvider
import com.example.financesharing.domain.model.GiftEventDomain
import com.example.financesharing.domain.model.Currency
import com.example.financesharing.domain.repository.GiftEventRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FirestoreGiftEventRepository : GiftEventRepository {
    private val auth = FirebaseProvider.auth
    private val firestore = FirebaseProvider.firestore

    override fun getActiveEvents(): Flow<List<GiftEventDomain>> =
        observeMyEvents { it.currentAmount < it.targetAmount }

    override fun getCompletedEvents(): Flow<List<GiftEventDomain>> =
        observeMyEvents { it.currentAmount >= it.targetAmount }

    private fun observeMyEvents(filter: (GiftEventDomain) -> Boolean): Flow<List<GiftEventDomain>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ownerQuery = firestore.collection("events")
            .whereEqualTo("ownerUid", uid)

        val participantQuery = firestore.collection("events")
            .whereArrayContains("participantUids", uid)

        var ownerDocs: List<GiftEventDomain> = emptyList()
        var participantDocs: List<GiftEventDomain> = emptyList()

        fun emitMerged() {
            val merged = (ownerDocs + participantDocs)
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<GiftEventDomain> { it.deadline }
                        .thenByDescending { it.id }
                )
                .filter(filter)
            trySend(merged)
        }

        val reg1 = ownerQuery.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            ownerDocs = snap?.documents?.mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                val description = doc.getString("description") ?: ""
                val currency = Currency.fromCode(doc.getString("currency"))
                val targetMinor = doc.getLong("targetAmount") ?: 0L
                val currentMinor = doc.getLong("currentAmount") ?: 0L
                val deadlineTs = doc.getTimestamp("deadline") ?: Timestamp.now()
                val deadline = Instant.ofEpochSecond(deadlineTs.seconds).atZone(ZoneId.systemDefault()).toLocalDate()
                val participantUids = (doc.get("participantUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                GiftEventDomain(
                    id = doc.id,
                    title = title,
                    description = description,
                    targetAmount = targetMinor.toDouble() / 100.0,
                    currentAmount = currentMinor.toDouble() / 100.0,
                    deadline = deadline,
                    participantsCount = participantUids.size,
                    imageUrl = null
                )
            } ?: emptyList()
            emitMerged()
        }

        val reg2 = participantQuery.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            participantDocs = snap?.documents?.mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                val description = doc.getString("description") ?: ""
                val targetMinor = doc.getLong("targetAmount") ?: 0L
                val currentMinor = doc.getLong("currentAmount") ?: 0L
                val deadlineTs = doc.getTimestamp("deadline") ?: Timestamp.now()
                val deadline = Instant.ofEpochSecond(deadlineTs.seconds).atZone(ZoneId.systemDefault()).toLocalDate()
                val participantUids = (doc.get("participantUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                GiftEventDomain(
                    id = doc.id,
                    title = title,
                    description = description,
                    targetAmount = targetMinor.toDouble() / 100.0,
                    currentAmount = currentMinor.toDouble() / 100.0,
                    deadline = deadline,
                    participantsCount = participantUids.size,
                    imageUrl = null
                )
            } ?: emptyList()
            emitMerged()
        }

        awaitClose {
            reg1.remove()
            reg2.remove()
        }
    }
}

