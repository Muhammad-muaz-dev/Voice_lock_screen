package com.futurecode.voicelockscreen.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.activity.LockScreenActivity
import com.futurecode.voicelockscreen.security.AppLockState

class LockService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    AppLockState.setUnlockedThisSession(context, false)
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (!AppLockState.hasAnyLockEnabled(context)) return
                    AppLockState.setUnlockedThisSession(context, false)
                    mainHandler.postDelayed({ launchLockScreen() }, SCREEN_ON_LAUNCH_DELAY_MS)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!AppLockState.hasAnyLockEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_SHOW_LOCK_SCREEN) {
            launchLockScreen()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenStateReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            this,
            screenStateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun launchLockScreen() {
        if (!AppLockState.hasAnyLockEnabled(this)) return
        if (AppLockState.isUnlockedThisSession(this)) return

        runCatching {
            startActivity(LockScreenActivity.newIntent(this))
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.lock_monitor_running))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
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
        private const val SCREEN_ON_LAUNCH_DELAY_MS = 250L
        const val ACTION_SHOW_LOCK_SCREEN = "com.futurecode.voicelockscreen.SHOW_LOCK_SCREEN"

        fun start(context: Context) {
            val intent = Intent(context, LockService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun startIfNeeded(context: Context) {
            if (!AppLockState.hasAnyLockEnabled(context)) return
            start(context)
        }

        fun showLockScreen(context: Context) {
            if (!AppLockState.hasAnyLockEnabled(context)) return
            val intent = Intent(context, LockService::class.java).apply {
                action = ACTION_SHOW_LOCK_SCREEN
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
