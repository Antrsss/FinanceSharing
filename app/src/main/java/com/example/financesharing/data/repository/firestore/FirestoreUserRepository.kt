package com.example.financesharing.data.repository.firestore

import com.example.financesharing.data.firebase.FirebaseProvider
import com.example.financesharing.domain.model.Currency
import com.example.financesharing.domain.model.UserProfile
import com.example.financesharing.domain.utils.BonusCalculator
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository {
    private val auth = FirebaseProvider.auth
    private val firestore = FirebaseProvider.firestore

    fun currentUidOrNull(): String? = auth.currentUser?.uid

    fun observeMyProfile(): Flow<UserProfile?> = callbackFlow {
        val uid = currentUidOrNull()
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val reg = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toUserProfile(uid))
            }

        awaitClose { reg.remove() }
    }

    suspend fun setAvatar(avatarId: String) {
        val uid = currentUidOrNull() ?: error("Not logged in")
        firestore.collection("users").document(uid)
            .update("avatarId", avatarId)
            .await()
    }

    suspend fun topUpWallet(amountMinor: Long) {
        require(amountMinor > 0) { "amountMinor must be > 0" }
        val uid = currentUidOrNull() ?: error("Not logged in")
        val ref = firestore.collection("users").document(uid)

        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val current = snap.getLong("walletBalance") ?: 0L
            tx.update(ref, "walletBalance", current + amountMinor)
            null
        }.await()
    }

    suspend fun addSpent(amountMinor: Long) {
        require(amountMinor > 0) { "amountMinor must be > 0" }
        val uid = currentUidOrNull() ?: error("Not logged in")
        val ref = firestore.collection("users").document(uid)

        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val spent = snap.getLong("spentTotal") ?: 0L
            val newSpent = spent + amountMinor
            val bonus = BonusCalculator.calculateBonusPoints(newSpent)
            tx.update(
                ref,
                mapOf(
                    "spentTotal" to newSpent,
                    "bonusPoints" to bonus
                )
            )
            null
        }.await()
    }

    suspend fun spendFromWallet(amountMinor: Long) {
        require(amountMinor > 0) { "amountMinor must be > 0" }
        val uid = currentUidOrNull() ?: error("Not logged in")
        val ref = firestore.collection("users").document(uid)

        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val balance = snap.getLong("walletBalance") ?: 0L
            if (balance < amountMinor) {
                throw IllegalStateException("Недостаточно средств в кошельке")
            }

            val spent = snap.getLong("spentTotal") ?: 0L
            val newSpent = spent + amountMinor
            val bonus = BonusCalculator.calculateBonusPoints(newSpent)

            tx.update(
                ref,
                mapOf(
                    "walletBalance" to (balance - amountMinor),
                    "spentTotal" to newSpent,
                    "bonusPoints" to bonus
                )
            )
            null
        }.await()
    }

    private fun DocumentSnapshot.toUserProfile(uid: String): UserProfile? {
        if (!exists()) return null
        val username = getString("username") ?: return null
        val email = getString("email") ?: ""
        val avatarId = getString("avatarId") ?: "avatar_1"
        val baseCurrency = Currency.fromCode(getString("baseCurrency"))
        val walletBalance = getLong("walletBalance") ?: 0L
        val spentTotal = getLong("spentTotal") ?: 0L
        val bonusPoints = getLong("bonusPoints") ?: BonusCalculator.calculateBonusPoints(spentTotal)
        return UserProfile(
            uid = uid,
            username = username,
            email = email,
            avatarId = avatarId,
            baseCurrency = baseCurrency,
            walletBalanceMinor = walletBalance,
            spentTotalMinor = spentTotal,
            bonusPoints = bonusPoints
        )
    }
}

