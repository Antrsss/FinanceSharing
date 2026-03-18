package com.example.financesharing.data.repository.firestore

import com.example.financesharing.data.firebase.FirebaseProvider
import com.example.financesharing.domain.model.Invitation
import com.example.financesharing.domain.model.InvitationStatus
import com.example.financesharing.domain.model.ParticipantStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreInvitationRepository {
    private val auth = FirebaseProvider.auth
    private val firestore = FirebaseProvider.firestore

    private fun uid(): String = auth.currentUser?.uid ?: error("Not logged in")

    fun observePendingInvitations(): Flow<List<Invitation>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Avoid composite index requirement: don't use orderBy with multiple filters.
        val query = firestore.collection("invitations")
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", InvitationStatus.PENDING.name)

        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val list = (snap?.documents?.mapNotNull { it.toInvitation() } ?: emptyList())
                .sortedByDescending { it.createdAtEpochMillis }
            trySend(list)
        }

        awaitClose { reg.remove() }
    }

    suspend fun respondToInvitation(invitationId: String, accept: Boolean) {
        val currentUid = uid()
        val invRef = firestore.collection("invitations").document(invitationId)

        firestore.runTransaction { tx ->
            val invSnap = tx.get(invRef)
            val toUid = invSnap.getString("toUid") ?: error("Invalid invitation")
            if (toUid != currentUid) error("Forbidden")

            val eventId = invSnap.getString("eventId") ?: error("Invalid invitation")
            val eventRef = firestore.collection("events").document(eventId)
            val participantRef = eventRef.collection("participants").document(currentUid)

            val userSnap = if (accept) tx.get(firestore.collection("users").document(currentUid)) else null

            val newInvStatus = if (accept) InvitationStatus.ACCEPTED else InvitationStatus.DECLINED
            val newPartStatus = if (accept) ParticipantStatus.ACCEPTED else ParticipantStatus.DECLINED

            tx.update(invRef, "status", newInvStatus.name)

            if (accept) {
                val username = (userSnap?.getString("username") ?: "").trim()
                if (username.isNotBlank()) {
                    tx.update(
                        participantRef,
                        mapOf(
                            "status" to newPartStatus.name,
                            "username" to username
                        )
                    )
                } else {
                    tx.update(participantRef, "status", newPartStatus.name)
                }
            } else {
                tx.update(participantRef, "status", newPartStatus.name)
            }

            tx.update(eventRef, "invitedUids", FieldValue.arrayRemove(currentUid))
            if (accept) {
                tx.update(eventRef, "participantUids", FieldValue.arrayUnion(currentUid))
            }
            null
        }.await()
    }

    private fun DocumentSnapshot.toInvitation(): Invitation? {
        if (!exists()) return null
        val eventId = getString("eventId") ?: return null
        val eventTitle = getString("eventTitle") ?: ""
        val fromUid = getString("fromUid") ?: return null
        val toUid = getString("toUid") ?: return null
        val statusStr = getString("status") ?: InvitationStatus.PENDING.name
        val status = runCatching { InvitationStatus.valueOf(statusStr) }.getOrDefault(InvitationStatus.PENDING)
        val createdAt = getTimestamp("createdAt") ?: Timestamp.now()
        return Invitation(
            id = id,
            eventId = eventId,
            eventTitle = eventTitle,
            fromUid = fromUid,
            toUid = toUid,
            status = status,
            createdAtEpochMillis = createdAt.toDate().time
        )
    }
}

