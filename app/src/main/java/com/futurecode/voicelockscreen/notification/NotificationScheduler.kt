package com.futurecode.voicelockscreen.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.futurecode.voicelockscreen.activity.MyApplication
import java.util.concurrent.TimeUnit
import kotlin.collections.any

object NotificationScheduler {

    private const val WORK_NAME = "voice_lock_notification_chain"

    fun startNotificationWorker(context: Context): Boolean {
        return try {
            val workManager = WorkManager.getInstance(context)

            workManager.cancelUniqueWork(WORK_NAME)

            val firstWorker = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(0, TimeUnit.MINUTES)
                .addTag("voice_lock_notification_worker")
                .build()

            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                firstWorker
            )

            val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
            val isStarted = workInfos.any {
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
            }

            MyApplication.app.prefManager.isNotificationStarts = isStarted
            isStarted
        } catch (e: Exception) {
            e.printStackTrace()
            MyApplication.app.prefManager.isNotificationStarts = false
            false
        }
    }
}