package com.futurecode.voicelockscreen.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.receiver.PhoneUnlockReceiver
import com.futurecode.voicelockscreen.security.AppLockState

class LockMonitorService : Service() {
    private val phoneUnlockReceiver = PhoneUnlockReceiver()
    private lateinit var lockOverlayController: LockOverlayController

    override fun onCreate() {
        super.onCreate()
        lockOverlayController = LockOverlayController(this)
        registerPhoneUnlockReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!AppLockState.hasAnyLockEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        if (intent?.action == ACTION_SHOW_LOCK_OVERLAY) {
            lockOverlayController.show()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (::lockOverlayController.isInitialized) {
            lockOverlayController.dismiss()
        }
        runCatching { unregisterReceiver(phoneUnlockReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerPhoneUnlockReceiver() {
        ContextCompat.registerReceiver(
            this,
            phoneUnlockReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, HomeActivity::class.java).apply {
                putExtra(HomeActivity.EXTRA_SHOW_LOCK_PREVIEW, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.lock_monitor_running))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.lock_monitor_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "lock_monitor_channel"
        private const val NOTIFICATION_ID = 1002
        const val ACTION_SHOW_LOCK_OVERLAY = "com.futurecode.voicelockscreen.SHOW_LOCK_OVERLAY"

        fun startIfNeeded(context: Context) {
            if (!AppLockState.hasAnyLockEnabled(context)) return

            val intent = Intent(context, LockMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun showLockOverlay(context: Context) {
            if (!AppLockState.hasAnyLockEnabled(context)) return

            val intent = Intent(context, LockMonitorService::class.java).apply {
                action = ACTION_SHOW_LOCK_OVERLAY
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
