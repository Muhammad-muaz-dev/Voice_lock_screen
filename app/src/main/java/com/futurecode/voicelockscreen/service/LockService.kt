package com.futurecode.voicelockscreen.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.AlarmManager
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.app.ServiceCompat
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.activity.LockScreenActivity
import com.futurecode.voicelockscreen.receiver.BootReceiver
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.utils.LockPermissionHelper
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore

class LockService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var lockOverlayWindow: LockOverlayWindow
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
        lockOverlayWindow = LockOverlayWindow(this)
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startAsForeground()

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
        if (::lockOverlayWindow.isInitialized) {
            lockOverlayWindow.dismiss()
        }
        runCatching { unregisterReceiver(screenStateReceiver) }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scheduleServiceRestart()
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

        // App overlays are hidden while the system keyguard is showing, so the
        // overlay is only usable when the device has no keyguard to cover it.
        if (!isKeyguardLocked() && LockPermissionHelper.hasOverlayPermission(this)) {
            lockOverlayWindow.show()
            return
        }

        lockOverlayWindow.dismiss()
        val launched = runCatching {
            startActivity(LockScreenActivity.newIntent(this))
        }.isSuccess

        // Background activity starts can be blocked silently on Android 10+, so the
        // full-screen intent guarantees the lock activity appears over the keyguard.
        if (!launched || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showFullScreenLockNotification()
        }
    }

    private fun isKeyguardLocked(): Boolean {
        return getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
    }

    private fun startAsForeground() {
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), serviceType)
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
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun showFullScreenLockNotification() {
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            FULL_SCREEN_REQUEST_CODE,
            LockScreenActivity.newIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            pendingIntentActivityOptions()
        )

        val notification = NotificationCompat.Builder(this, LOCK_ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(LOCK_ALERT_NOTIFICATION_ID, notification)
    }

    private fun pendingIntentActivityOptions(): Bundle? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ActivityOptions.makeBasic().apply {
                pendingIntentCreatorBackgroundActivityStartMode =
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }.toBundle()
        } else {
            null
        }
    }

    private fun scheduleServiceRestart() {
        if (!AppLockState.hasAnyLockEnabled(this)) return

        val restartIntent = Intent(this, BootReceiver::class.java).apply {
            action = BootReceiver.ACTION_RESTART_LOCK_SERVICE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            RESTART_REQUEST_CODE,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = SystemClock.elapsedRealtime() + RESTART_DELAY_MS
        val alarmManager = getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val monitorChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            setShowBadge(false)
        }
        val alertChannel = NotificationChannel(
            LOCK_ALERT_CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(monitorChannel)
            createNotificationChannel(alertChannel)
        }
    }

    companion object {
        private const val TAG = "LockService"
        private const val CHANNEL_ID ="lock_monitor_silent_channel_v2"
        private const val LOCK_ALERT_CHANNEL_ID = "lock_alert_channel_v2"
        private const val NOTIFICATION_ID = 1002
        const val LOCK_ALERT_NOTIFICATION_ID = 1003
        private const val FULL_SCREEN_REQUEST_CODE = 301
        private const val RESTART_REQUEST_CODE = 302
        private const val RESTART_DELAY_MS = 1000L
        private const val SCREEN_ON_LAUNCH_DELAY_MS = 250L
        const val ACTION_SHOW_LOCK_SCREEN = "com.futurecode.voicelockscreen.SHOW_LOCK_SCREEN"

        fun start(context: Context) {
            startForegroundSafely(context, Intent(context, LockService::class.java))
        }

        // Android 12+ refuses foreground-service starts from the background (e.g. when the
        // process is spun up for a job); the boot/restart paths retry later instead of crashing.
        private fun startForegroundSafely(context: Context, intent: Intent) {
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Log.w(TAG, "Lock service start refused", it) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockService::class.java))
        }

        fun startIfNeeded(context: Context) {
            if (!AppLockState.hasAnyLockEnabled(context)) return
            start(context)
        }

        fun enableAndStart(context: Context) {
            VoiceLockSwitchStore.setUnlockEnabled(context, true)
            startIfNeeded(context)
        }

        fun showLockScreen(context: Context) {
            if (!AppLockState.hasAnyLockEnabled(context)) return
            val intent = Intent(context, LockService::class.java).apply {
                action = ACTION_SHOW_LOCK_SCREEN
            }
            startForegroundSafely(context, intent)
        }
    }
}
