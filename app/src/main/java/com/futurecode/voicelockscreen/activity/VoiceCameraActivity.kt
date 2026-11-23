package com.futurecode.voicelockscreen.activity

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.TextureView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ActivityVoiceCameraBinding
import com.futurecode.voicelockscreen.utils.CameraCommand
import com.futurecode.voicelockscreen.service.CameraVoiceService
import com.futurecode.voicelockscreen.utils.SpeechSession
import java.io.File
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceCameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVoiceCameraBinding
    private lateinit var camera: VoiceCameraController
    private var visible = false
    private var ready = false
    private var flashAvailable = false
    private var flipAvailable = false
    private var flash = false
    private var videoMode = false
    private var busy = false
    private var recording = false
    private var pendingCapture: CameraCommand? = null

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && visible) openIfReady()
        if (!granted) {
            toast(R.string.camera_permission_denied)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            binding.cameraControls.updatePadding(
                left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom
            )
            insets
        }
        camera = VoiceCameraController(this, binding.livePreview, ::updateCameraState) {
            pendingCapture = null
            busy = false
            recording = false
            render()
            toast(R.string.voice_camera_error)
        }
        binding.livePreview.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) = openIfReady()
            override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit
            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                closeCamera()
                return true
            }
        }
        binding.closeCamera.setOnClickListener { finish() }
        binding.cameraFlash.setOnClickListener {
            flash = camera.toggleFlash()
            render()
        }
        binding.flipCamera.setOnClickListener {
            flash = false
            camera.flip()
            render()
        }
        binding.cameraPhoto.setOnClickListener { setVideoMode(false) }
        binding.cameraVideo.setOnClickListener { setVideoMode(true) }
        binding.cameraShutter.setOnClickListener {
            when {
                recording -> stopRecording()
                videoMode -> startRecording()
                else -> capturePhoto()
            }
        }
        videoMode = savedInstanceState?.getBoolean(KEY_VIDEO) ?: (command(intent) == CameraCommand.VIDEO)
        render()
        if (!hasCameraPermission()) permission.launch(Manifest.permission.CAMERA)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCommand(command(intent))
    }

    override fun onResume() {
        super.onResume()
        visible = true
        openIfReady()
        CameraVoiceService.setCameraCommandHandler(::handleCommand)
    }

    override fun onPause() {
        visible = false
        CameraVoiceService.setCameraCommandHandler(null)
        pendingCapture = null
        closeCamera()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_VIDEO, videoMode)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun openIfReady() {
        if (visible && binding.livePreview.isAvailable && hasCameraPermission()) camera.open()
    }

    private fun closeCamera() {
        if (!::camera.isInitialized) return
        camera.close()?.let(::saveVideo)
        busy = false
        recording = false
        render()
    }

    private fun handleCommand(command: CameraCommand) {
        when (command) {
            CameraCommand.OPEN -> Unit
            CameraCommand.PHOTO -> setVideoMode(false)
            CameraCommand.VIDEO -> setVideoMode(true)
            CameraCommand.CAPTURE, CameraCommand.RECORD -> {
                if (busy || recording) return
                setVideoMode(command == CameraCommand.RECORD)
                if (!ready) {
                    pendingCapture = command
                } else if (command == CameraCommand.RECORD) {
                    startRecording()
                } else {
                    capturePhoto()
                }
            }
            CameraCommand.STOP -> {
                pendingCapture = null
                stopRecording()
            }
        }
    }

    private fun setVideoMode(video: Boolean) {
        if (busy || recording) return
        pendingCapture = null
        videoMode = video
        render()
    }

    private fun capturePhoto() {
        if (!ready || busy || recording) return
        busy = true
        render()
        camera.takePhoto { bytes ->
            busy = false
            render()
            saveMedia(false) { it.write(bytes) }
        }
    }

    private fun startRecording() {
        if (!ready || busy || recording) return
        val file = runCatching { File.createTempFile("voice-camera-", ".mp4", cacheDir) }
            .getOrElse { toast(R.string.voice_camera_save_error); return }
        busy = true
        render()
        camera.startVideo(file, withAudio = hasVideoAudio()) {
            busy = false
            recording = true
            render()
        }
    }

    private fun stopRecording() {
        if (!recording) return
        ready = false
        recording = false
        val file = camera.stopVideo(restorePreview = true)
        if (file == null) toast(R.string.voice_camera_save_error) else saveVideo(file)
        render()
    }

    private fun saveVideo(file: File) = saveMedia(true, onComplete = { file.delete() }) { output ->
        file.inputStream().use { it.copyTo(output) }
    }

    private fun saveMedia(video: Boolean, onComplete: () -> Unit = {}, write: (OutputStream) -> Unit) {
        val context = applicationContext
        saves.launch {
            val result = runCatching { writeMedia(context, video, write) }
            onComplete()
            withContext(Dispatchers.Main) {
                val message = if (result.isSuccess) R.string.voice_camera_saved else R.string.voice_camera_save_error
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCameraState(ready: Boolean, canFlash: Boolean, canFlip: Boolean) {
        this.ready = ready
        flashAvailable = canFlash
        flipAvailable = canFlip
        render()
        if (ready) {
            val command = pendingCapture
            pendingCapture = null
            command?.let(::handleCommand)
        }
    }

    private fun hasVideoAudio() = !CameraVoiceService.isRunning && SpeechSession.hasMicPermission(this)

    private fun render() {
        binding.cameraPhoto.isSelected = !videoMode
        binding.cameraVideo.isSelected = videoMode
        binding.cameraPhoto.isEnabled = !busy && !recording
        binding.cameraVideo.isEnabled = !busy && !recording
        binding.cameraFlash.isEnabled = ready && flashAvailable && !busy && !recording
        binding.cameraFlash.isSelected = flash && flashAvailable
        binding.cameraFlash.alpha = when {
            !flashAvailable -> 0.38f
            flash -> 1f
            else -> 0.65f
        }
        binding.cameraFlash.contentDescription = getString(
            if (flash && flashAvailable) R.string.voice_camera_flash_on else R.string.voice_camera_flash_off
        )
        binding.flipCamera.isEnabled = ready && flipAvailable && !busy && !recording
        binding.flipCamera.alpha = if (binding.flipCamera.isEnabled) 1f else 0.38f
        binding.cameraShutter.isEnabled = ready && !busy
        binding.cameraShutter.isSelected = recording
        binding.cameraShutter.contentDescription = getString(when {
            recording -> R.string.voice_camera_stop_recording
            videoMode -> R.string.voice_camera_start_recording
            else -> R.string.voice_camera_take_photo
        })
        binding.cameraStatus.text = when {
            recording && hasVideoAudio() -> getString(R.string.voice_camera_recording_with_audio)
            recording -> getString(R.string.voice_camera_recording)
            videoMode && !hasVideoAudio() -> getString(R.string.voice_camera_silent_video)
            else -> ""
        }
    }

    private fun toast(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val KEY_VIDEO = "voice_camera_video"
        private val saves = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun newIntent(context: Context, command: CameraCommand = CameraCommand.OPEN) =
            Intent(context, VoiceCameraActivity::class.java).putExtra(KEY_VIDEO, command == CameraCommand.VIDEO)

        private fun command(intent: Intent) =
            if (intent.getBooleanExtra(KEY_VIDEO, false)) CameraCommand.VIDEO else CameraCommand.PHOTO

        private fun writeMedia(context: Context, video: Boolean, write: (OutputStream) -> Unit) {
            val name = "VoiceLock_${System.currentTimeMillis()}" + if (video) ".mp4" else ".jpg"
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val directory = context.getExternalFilesDir(if (video) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES)
                    ?: context.filesDir
                val file = File(directory, name)
                runCatching { file.outputStream().use(write) }.getOrElse {
                    file.delete()
                    throw it
                }
                return
            }
            val collection = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, if (video) "video/mp4" else "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, (if (video) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES) + "/VoiceLock")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = requireNotNull(resolver.insert(collection, values))
            runCatching {
                requireNotNull(resolver.openOutputStream(uri)).use(write)
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                check(resolver.update(uri, values, null, null) == 1)
            }.getOrElse {
                resolver.delete(uri, null, null)
                throw it
            }
        }
    }
}
