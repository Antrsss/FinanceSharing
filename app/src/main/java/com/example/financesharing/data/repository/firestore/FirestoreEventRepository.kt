package com.example.financesharing.data.repository.firestore

import com.example.financesharing.data.firebase.FirebaseProvider
import com.example.financesharing.domain.model.Contribution
import com.example.financesharing.domain.model.Contributor
import com.example.financesharing.domain.model.Currency
import com.example.financesharing.domain.model.GiftEvent
import com.example.financesharing.domain.model.InvitationStatus
import com.example.financesharing.domain.model.Participant
import com.example.financesharing.domain.model.ParticipantStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class FirestoreEventRepository(
    private val userRepository: FirestoreUserRepository = FirestoreUserRepository()
) {
    private val auth = FirebaseProvider.auth
    private val firestore = FirebaseProvider.firestore

    private fun uid(): String = auth.currentUser?.uid ?: error("Not logged in")

    fun observeMyEvents(): Flow<List<GiftEvent>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ownerQuery = firestore.collection("events")
            .whereEqualTo("ownerUid", currentUid)

        val participantQuery = firestore.collection("events")
            .whereArrayContains("participantUids", currentUid)

        var ownerDocs: List<GiftEvent> = emptyList()
        var participantDocs: List<GiftEvent> = emptyList()

        fun emitMerged() {
            val merged = (ownerDocs + participantDocs)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAtEpochMillis }
            trySend(merged)
        }

        val reg1 = ownerQuery.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            ownerDocs = snap?.documents?.mapNotNull { it.toGiftEvent() } ?: emptyList()
            emitMerged()
        }
        val reg2 = participantQuery.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            participantDocs = snap?.documents?.mapNotNull { it.toGiftEvent() } ?: emptyList()
            emitMerged()
        }

        awaitClose {
            reg1.remove()
            reg2.remove()
        }
    }

    fun observeEvent(eventId: String): Flow<GiftEvent?> = callbackFlow {
        val reg = firestore.collection("events").document(eventId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snap?.toGiftEvent())
            }
        awaitClose { reg.remove() }
    }

    fun observeParticipants(eventId: String): Flow<List<Participant>> = callbackFlow {
        val reg = firestore.collection("events")
            .document(eventId)
            .collection("participants")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents?.mapNotNull { it.toParticipant() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeContributions(eventId: String): Flow<List<Contribution>> = callbackFlow {
        val reg = firestore.collection("events")
            .document(eventId)
            .collection("contributions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents?.mapNotNull { it.toContribution() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createEvent(
        title: String,
        description: String,
        currency: Currency,
        targetAmountMinor: Long,
        deadline: LocalDate
    ): String {
        val uid = uid()
        val eventRef = firestore.collection("events").document()
        val participantRef = eventRef.collection("participants").document(uid)

        val now = Timestamp.now()
        val doc = mapOf(
            "ownerUid" to uid,
            "title" to title.trim(),
            "description" to description.trim(),
            "currency" to currency.code,
            "targetAmount" to targetAmountMinor,
            "currentAmount" to 0L,
            "deadline" to Timestamp(deadline.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond, 0),
            "createdAt" to now,
            // for Home queries
            "participantUids" to listOf(uid),
            "invitedUids" to emptyList<String>()
        )

        val ownerParticipant = mapOf(
            "type" to "user",
            "uid" to uid,
            "username" to (auth.currentUser?.email ?: "me"),
            "status" to ParticipantStatus.ACCEPTED.name
        )

        // Important: creating /participants in the same batch as /events can be rejected by rules
        // because rules use get(/events/{eventId}) which doesn't "see" the batched create.
        eventRef.set(doc).await()
        participantRef.set(ownerParticipant).await()

        return eventRef.id
    }

    suspend fun addGuestContribution(
        eventId: String,
        guestName: String,
        amountMinor: Long
    ) {
        require(amountMinor > 0) { "amountMinor must be > 0" }
        val eventRef = firestore.collection("events").document(eventId)
        val contRef = eventRef.collection("contributions").document()
        val now = Timestamp.now()

        firestore.runBatch { batch ->
            batch.set(
                contRef,
                mapOf(
                    "contributorType" to "guest",
                    "displayName" to guestName.trim(),
                    "amount" to amountMinor,
                    "createdAt" to now
                )
            )
            batch.update(eventRef, "currentAmount", FieldValue.increment(amountMinor))
        }.await()
    }

    suspend fun addUserContribution(
        eventId: String,
        amountMinor: Long
    ) {
        require(amountMinor > 0) { "amountMinor must be > 0" }
        val uid = uid()
        val username = auth.currentUser?.email ?: "user"

        val eventRef = firestore.collection("events").document(eventId)
        val contRef = eventRef.collection("contributions").document()
        val now = Timestamp.now()

        // Wallet check & spend first (MVP: 1:1, no currency conversion)
        userRepository.spendFromWallet(amountMinor)

        runCatching {
            firestore.runBatch { batch ->
                batch.set(
                    contRef,
                    mapOf(
                        "contributorType" to "user",
                        "uid" to uid,
                        "displayName" to username,
                        "amount" to amountMinor,
                        "createdAt" to now
                    )
                )
                batch.update(eventRef, "currentAmount", FieldValue.increment(amountMinor))
            }.await()
        }.getOrElse { t ->
            // Best-effort refund if contribution write fails after wallet spend
            runCatching { userRepository.topUpWallet(amountMinor) }
            throw t
        }
    }

    suspend fun findUserUidByUsernameOrEmail(query: String): String? {
        val q = query.trim()
        if (q.isBlank()) return null
        val key = q.lowercase()

        val usernameDoc = firestore.collection("usernames").document(key).get().await()
        val u1 = usernameDoc.getString("uid")
        if (!u1.isNullOrBlank()) return u1

        // Email lookup is also supported via exact-key index
        val emailDoc = firestore.collection("emails").document(key).get().await()
        return emailDoc.getString("uid")
    }

    suspend fun inviteUser(
        eventId: String,
        toUid: String,
        eventTitle: String
    ): String {
        val fromUid = uid()
        require(toUid != fromUid) { "Cannot invite yourself" }

        val invitationId = UUID.randomUUID().toString()
        val invRef = firestore.collection("invitations").document(invitationId)

        val eventRef = firestore.collection("events").document(eventId)
        val participantRef = eventRef.collection("participants").document(toUid)

        val now = Timestamp.now()
        firestore.runBatch { batch ->
            batch.set(
                invRef,
                mapOf(
                    "eventId" to eventId,
                    "eventTitle" to eventTitle,
                    "fromUid" to fromUid,
                    "toUid" to toUid,
                    "status" to InvitationStatus.PENDING.name,
                    "createdAt" to now
                )
            )
            batch.set(
                participantRef,
                mapOf(
                    "type" to "user",
                    "uid" to toUid,
                    "username" to "",
                    "status" to ParticipantStatus.INVITED.name
                )
            )
            batch.update(eventRef, "invitedUids", FieldValue.arrayUnion(toUid))
        }.await()

        return invitationId
    }

    private fun DocumentSnapshot.toGiftEvent(): GiftEvent? {
        if (!exists()) return null
        val id = id
        val ownerUid = getString("ownerUid") ?: return null
        val title = getString("title") ?: return null
        val description = getString("description") ?: ""
        val currency = Currency.fromCode(getString("currency"))
        val target = getLong("targetAmount") ?: 0L
        val current = getLong("currentAmount") ?: 0L
        val deadlineTs = getTimestamp("deadline") ?: Timestamp.now()
        val createdAt = getTimestamp("createdAt") ?: Timestamp.now()

        val deadline = Instant.ofEpochSecond(deadlineTs.seconds)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return GiftEvent(
            id = id,
            ownerUid = ownerUid,
            title = title,
            description = description,
            currency = currency,
            targetAmountMinor = target,
            currentAmountMinor = current,
            deadline = deadline,
            createdAtEpochMillis = createdAt.toDate().time
        )
    }

    private fun DocumentSnapshot.toParticipant(): Participant? {
        if (!exists()) return null
        val type = getString("type") ?: return null
        val statusStr = getString("status") ?: ParticipantStatus.ACCEPTED.name
        val status = runCatching { ParticipantStatus.valueOf(statusStr) }.getOrDefault(ParticipantStatus.ACCEPTED)
        return when (type) {
            "user" -> {
                val uid = getString("uid") ?: return null
                val username = getString("username") ?: ""
                Participant.User(
                    id = id,
                    uid = uid,
                    username = username.ifBlank { uid.take(6) },
                    status = status
                )
            }

            "guest" -> {
                val displayName = getString("displayName") ?: return null
                Participant.Guest(
                    id = id,
                    displayName = displayName,
                    status = status
                )
            }

            else -> null
        }
    }

    private fun DocumentSnapshot.toContribution(): Contribution? {
        if (!exists()) return null
        val type = getString("contributorType") ?: return null
        val amount = getLong("amount") ?: 0L
        val createdAt = getTimestamp("createdAt") ?: Timestamp.now()

        val contributor = when (type) {
            "user" -> {
                val uid = getString("uid") ?: return null
                val username = getString("displayName") ?: uid.take(6)
                Contributor.User(uid = uid, username = username)
            }

            "guest" -> {
                val name = getString("displayName") ?: return null
                Contributor.Guest(displayName = name)
            }

            else -> return null
        }

        return Contribution(
            id = id,
            contributor = contributor,
            amountMinor = amount,
            createdAtEpochMillis = createdAt.toDate().time
        )
    }
}

