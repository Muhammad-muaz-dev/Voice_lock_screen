package com.futurecode.voicelockscreen.service

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.VoiceCameraActivity
import com.futurecode.voicelockscreen.utils.CameraCommand
import com.futurecode.voicelockscreen.utils.CameraSpeech
import com.futurecode.voicelockscreen.utils.LockPermissionHelper
import com.futurecode.voicelockscreen.utils.SpeechSession
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraVoiceService : Service(), DefaultLifecycleObserver {
    private val appLifecycle get() = ProcessLifecycleOwner.get().lifecycle
    private val notifications get() = getSystemService(NotificationManager::class.java)
    private val speech by lazy { CameraSpeech(this, ::handleCommand, ::speechUnavailable) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectivity get() = getSystemService(ConnectivityManager::class.java)
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            serviceScope.launch { refreshListening() }
        }

        override fun onLost(network: Network) {
            serviceScope.launch { refreshListening() }
        }
    }

    private var running = false
    private var lastLaunchAt: Long? = null
    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshListening()
    }

    override fun onCreate() {
        super<Service>.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel(
                NotificationChannel(CHANNEL, getString(R.string.camera_voice_channel), NotificationManager.IMPORTANCE_LOW)
            )
        }
        ContextCompat.registerReceiver(this, screen, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        connectivity.registerDefaultNetworkCallback(networkCallback)
        appLifecycle.addObserver(this)
        serviceScope.launch {
            setupListening.collectLatest { visible ->
                if (!visible) delay(350L)
                refreshListening()
            }
        }
        serviceScope.launch { cameraCommands.collect { refreshListening() } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            VoiceLockFeatureStore.setCameraEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = notification(R.string.camera_voice_enabled, CameraCommand.OPEN)
            .addAction(0, getString(R.string.camera_voice_stop), stopAction())
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else 0
        running = runCatching { ServiceCompat.startForeground(this, SERVICE_ID, notification, type) }
            .onFailure { Log.w(TAG, "Microphone service could not enter foreground", it) }
            .isSuccess
        runningState.value = running
        if (!running || !VoiceLockFeatureStore.isCameraEnabled(this)) {
            VoiceLockFeatureStore.setCameraEnabled(this, false)
            stopSelf()
        } else {
            refreshListening()
        }
        return START_NOT_STICKY
    }

    override fun onStart(owner: LifecycleOwner) {
        notifications.cancel(CAMERA_READY_ID)
        val cameraOpening = lastLaunchAt?.let { SystemClock.elapsedRealtime() - it < 2_000L } == true
        if (cameraOpening) {
            // Let the camera attach to the current recognition session during the window transition.
            serviceScope.launch {
                delay(700L)
                refreshListening()
            }
        } else {
            refreshListening()
        }
    }

    override fun onStop(owner: LifecycleOwner) = refreshListening()

    override fun onDestroy() {
        running = false
        runningState.value = false
        speech.stop()
        connectivity.unregisterNetworkCallback(networkCallback)
        serviceScope.cancel()
        appLifecycle.removeObserver(this)
        unregisterReceiver(screen)
        notifications.cancel(CAMERA_READY_ID)
        super<Service>.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun refreshListening() {
        val foreground = appLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        val locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if (shouldListen(
                running, foreground, setupListening.value, screenOn, locked,
                cameraVisible = cameraCommands.value != null,
                internetAvailable = CameraSpeech.hasInternet(this)
            )
        ) speech.start()
        else speech.stop()
    }

    private fun handleCommand(command: CameraCommand) {
        cameraCommands.value?.let { handle ->
            Log.i(TAG, "Camera control accepted: $command")
            handle(command)
            return
        }
        if (command != CameraCommand.OPEN) return
        val now = SystemClock.elapsedRealtime()
        if (lastLaunchAt?.let { now - it < 4_000L } == true) return
        lastLaunchAt = now
        Log.i(TAG, "Camera command accepted: $command")
        notifications.notify(CAMERA_READY_ID, notification(R.string.camera_voice_tap_to_open, command).setAutoCancel(true).build())
        if ((setupListening.value || LockPermissionHelper.hasOverlayPermission(this)) &&
            !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        ) {
            runCatching { startActivity(VoiceCameraActivity.newIntent(this, command).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { Log.w(TAG, "Camera activity launch failed", it) }
        }
    }

    private fun speechUnavailable() {
        VoiceLockFeatureStore.setCameraEnabled(this, false)
        val message = R.string.camera_voice_unavailable
        if (setupListening.value || cameraCommands.value != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        notifications.notify(CAMERA_ERROR_ID, notification(message, CameraCommand.OPEN).setAutoCancel(true).build())
        stopSelf()
    }

    private fun notification(title: Int, command: CameraCommand): NotificationCompat.Builder {
        val open = PendingIntent.getActivity(this, command.ordinal, VoiceCameraActivity.newIntent(this, command),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(title))
            .setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setSilent(true)
    }

    private fun stopAction() = PendingIntent.getService(
        this, SERVICE_ID, Intent(this, CameraVoiceService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val TAG = "CameraVoiceService"
        private const val CHANNEL = "voice_camera_listener"
        private const val SERVICE_ID = 1006
        private const val CAMERA_READY_ID = 1007
        private const val CAMERA_ERROR_ID = 1008

        private val cameraCommands = MutableStateFlow<((CameraCommand) -> Unit)?>(null)
        private val setupListening = MutableStateFlow(false)
        private val runningState = MutableStateFlow(false)
        val runningStatus = runningState.asStateFlow()
        val isRunning get() = runningState.value
        private const val ACTION_STOP = "stop_camera_voice"

        internal fun shouldListen(
            running: Boolean,
            appVisible: Boolean,
            setupVisible: Boolean,
            screenOn: Boolean,
            locked: Boolean,
            cameraVisible: Boolean = false,
            internetAvailable: Boolean = true
        ) = running && (!appVisible || setupVisible || cameraVisible) && screenOn && !locked && internetAvailable

        fun setCameraCommandHandler(handler: ((CameraCommand) -> Unit)?) {
            cameraCommands.value = handler
        }

        fun setSetupListening(allowed: Boolean) {
            setupListening.value = allowed
        }

        fun start(context: Context): Boolean {
            if (!CameraSpeech.hasInternet(context) || !CameraSpeech.isAvailable(context) || !SpeechSession.hasMicPermission(context)) return false
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return false
            val intent = Intent(context, CameraVoiceService::class.java)
            return runCatching { ContextCompat.startForegroundService(context, intent) }.isSuccess
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CameraVoiceService::class.java))
        }
    }
}
