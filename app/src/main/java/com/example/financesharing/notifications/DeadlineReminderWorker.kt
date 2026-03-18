package com.example.financesharing.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.financesharing.data.firebase.FirebaseProvider
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DeadlineReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseProvider.auth.currentUser?.uid ?: return Result.success()
        val firestore = FirebaseProvider.firestore

        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowKey = "deadline_notified_${tomorrow}"
        val prefs = applicationContext.getSharedPreferences("giftshare_notifications", Context.MODE_PRIVATE)
        val already = prefs.getStringSet(tomorrowKey, emptySet())?.toMutableSet() ?: mutableSetOf()

        val ownerSnap = firestore.collection("events")
            .whereEqualTo("ownerUid", uid)
            .get()
            .await()

        val partSnap = firestore.collection("events")
            .whereArrayContains("participantUids", uid)
            .get()
            .await()

        val docs = (ownerSnap.documents + partSnap.documents).distinctBy { it.id }
        for (doc in docs) {
            val title = doc.getString("title") ?: continue
            val deadlineTs = doc.getTimestamp("deadline") ?: Timestamp.now()
            val deadline = Instant.ofEpochSecond(deadlineTs.seconds).atZone(ZoneId.systemDefault()).toLocalDate()
            if (deadline != tomorrow) continue
            if (already.contains(doc.id)) continue

            AppNotifications.show(
                context = applicationContext,
                id = 2000 + (doc.id.hashCode() and 0x7fffffff) % 5000,
                title = "Дедлайн завтра",
                text = "Сбор «$title» заканчивается завтра"
            )
            already.add(doc.id)
        }

        prefs.edit().putStringSet(tomorrowKey, already).apply()
        return Result.success()
    }
}

