package com.example.financesharing.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.financesharing.data.firebase.FirebaseProvider
import com.example.financesharing.domain.model.InvitationStatus
import kotlinx.coroutines.tasks.await

class InvitationsPollWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseProvider.auth.currentUser?.uid ?: return Result.success()

        val firestore = FirebaseProvider.firestore
        val snap = firestore.collection("invitations")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", InvitationStatus.PENDING.name)
            .get()
            .await()

        val ids = snap.documents.map { it.id }.sorted()
        val prefs = applicationContext.getSharedPreferences("giftshare_notifications", Context.MODE_PRIVATE)
        val last = prefs.getString("last_invites_ids", "") ?: ""
        val now = ids.joinToString(",")

        if (now.isNotBlank() && now != last) {
            AppNotifications.show(
                context = applicationContext,
                id = 1001,
                title = "Новое приглашение",
                text = "У вас есть приглашение вступить в сбор"
            )
        }

        prefs.edit().putString("last_invites_ids", now).apply()
        return Result.success()
    }
}

