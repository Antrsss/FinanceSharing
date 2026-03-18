package com.example.financesharing.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val INVITES_WORK = "invites_poll_work"
    private const val DEADLINES_WORK = "deadlines_reminder_work"

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)

        val invites = PeriodicWorkRequestBuilder<InvitationsPollWorker>(15, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(INVITES_WORK, ExistingPeriodicWorkPolicy.UPDATE, invites)

        val deadlines = PeriodicWorkRequestBuilder<DeadlineReminderWorker>(12, TimeUnit.HOURS)
            .build()
        wm.enqueueUniquePeriodicWork(DEADLINES_WORK, ExistingPeriodicWorkPolicy.UPDATE, deadlines)
    }
}

