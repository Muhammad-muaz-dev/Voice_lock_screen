package com.futurecode.voicelockscreen.activity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.TextureView
import java.io.File
import kotlin.math.abs

/** Owns Camera2 resources while the camera activity is visible. */
internal class VoiceCameraController(
    private val context: Context,
    private val preview: TextureView,
    private val onState: (ready: Boolean, canFlash: Boolean, canFlip: Boolean) -> Unit,
    private val onError: () -> Unit
) {
    private val manager = context.getSystemService(CameraManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var imageReader: ImageReader? = null
    private var recorder: MediaRecorder? = null
    private var videoFile: File? = null
    private var videoStarted = false
    private var generation = 0
    private var cameraId: String? = null
    private var info: CameraCharacteristics? = null
    private var flash = false

    private val cameras
        get() = manager.cameraIdList.filter { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) in
                listOf(CameraCharacteristics.LENS_FACING_FRONT, CameraCharacteristics.LENS_FACING_BACK)
        }

    @SuppressLint("MissingPermission")
    fun open() {
        if (previewSurface != null) return
        val texture = preview.surfaceTexture ?: return
        close()
        val token = generation
        runCatching {
            val ids = cameras
            val id = cameraId?.takeIf { it in ids } ?: ids.firstOrNull {
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            } ?: ids.first()
            cameraId = id
            val characteristics = manager.getCameraCharacteristics(id)
            info = characteristics
            val configuration = requireNotNull(characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
            val previewSize = chooseSize(configuration.getOutputSizes(SurfaceTexture::class.java))
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            applyPreviewScale(previewSize)
            previewSurface = Surface(texture)
            val photoSize = chooseSize(configuration.getOutputSizes(ImageFormat.JPEG))
            imageReader = ImageReader.newInstance(photoSize.width, photoSize.height, ImageFormat.JPEG, 2)
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    if (token != generation) {
                        camera.close()
                        return
                    }
                    device = camera
                    configurePreview()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    if (camera === device || token == generation) fail()
                }

                override fun onError(camera: CameraDevice, error: Int) = onDisconnected(camera)
            }, handler)
        }.onFailure { fail() }
    }

    fun flip() {
        val facing = info?.get(CameraCharacteristics.LENS_FACING)
        val next = runCatching {
            cameras.firstOrNull { manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) != facing }
        }.getOrNull() ?: return
        cameraId = next
        flash = false
        close()
        open()
    }

    fun toggleFlash(): Boolean {
        flash = !flash
        return flash
    }

    fun takePhoto(onImage: (ByteArray) -> Unit) {
        val reader = imageReader ?: return
        val camera = device ?: return
        val activeSession = session ?: return
        reader.setOnImageAvailableListener({ source ->
            if (imageReader !== source) return@setOnImageAvailableListener
            source.acquireLatestImage()?.use { image ->
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                onImage(bytes)
            }
            source.setOnImageAvailableListener(null, null)
        }, handler)
        runCatching {
            val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation())
                if (flash) set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
            }
            activeSession.capture(request.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureFailed(
                    session: CameraCaptureSession, request: CaptureRequest, failure: android.hardware.camera2.CaptureFailure
                ) {
                    reader.setOnImageAvailableListener(null, null)
                    fail()
                }
            }, handler)
        }.onFailure { fail() }
    }

    @Suppress("DEPRECATION")
    fun startVideo(file: File, withAudio: Boolean, onStarted: () -> Unit) {
        val characteristics = info ?: return
        val surface = previewSurface ?: return
        videoFile = file
        runCatching {
            val configuration = requireNotNull(characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
            val size = chooseSize(configuration.getOutputSizes(MediaRecorder::class.java))
            val media = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            recorder = media
            if (withAudio) media.setAudioSource(MediaRecorder.AudioSource.MIC)
            media.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            media.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            media.setOutputFile(file.absolutePath)
            media.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (withAudio) media.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            media.setVideoSize(size.width, size.height)
            media.setVideoEncodingBitRate(6_000_000)
            media.setVideoFrameRate(30)
            media.setOrientationHint(sensorOrientation())
            media.prepare()
            configureSession(
                listOf(surface, media.surface),
                listOf(surface, media.surface),
                CameraDevice.TEMPLATE_RECORD
            ) {
                media.start()
                videoStarted = true
                onStarted()
            }
        }.onFailure { fail() }
    }

    fun stopVideo(restorePreview: Boolean): File? {
        if (recorder == null) return null
        session?.close()
        session = null
        val output = releaseRecorder()
        if (restorePreview) configurePreview()
        return output
    }

    fun close(): File? {
        generation++
        val output = stopVideo(restorePreview = false)
        session?.close()
        session = null
        device?.close()
        device = null
        imageReader?.close()
        imageReader = null
        previewSurface?.release()
        previewSurface = null
        onState(false, false, false)
        return output
    }

    private fun releaseRecorder(): File? {
        val media = recorder
        recorder = null
        val valid = videoStarted && runCatching { media?.stop() }.isSuccess
        videoStarted = false
        media?.release()
        val file = videoFile
        videoFile = null
        if (valid && file != null && file.length() > 0) return file
        file?.delete()
        return null
    }

    private fun configurePreview() {
        val surface = previewSurface ?: return
        val reader = imageReader ?: return
        configureSession(listOf(surface, reader.surface), listOf(surface), CameraDevice.TEMPLATE_PREVIEW) {
            val canFlash = info?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val facing = info?.get(CameraCharacteristics.LENS_FACING)
            val canFlip = cameras.any { manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) != facing }
            onState(true, canFlash, canFlip)
        }
    }

    @Suppress("DEPRECATION")
    private fun configureSession(outputs: List<Surface>, targets: List<Surface>, template: Int, onConfigured: () -> Unit) {
        val camera = device ?: return
        session?.close()
        session = null
        val token = ++generation
        runCatching {
            camera.createCaptureSession(outputs, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(active: CameraCaptureSession) {
                    if (token != generation || camera !== device) {
                        active.close()
                        return
                    }
                    session = active
                    runCatching {
                        val request = camera.createCaptureRequest(template).apply {
                            targets.forEach(::addTarget)
                            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                            if (template == CameraDevice.TEMPLATE_RECORD && flash) {
                                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                            }
                        }
                        active.setRepeatingRequest(request.build(), null, handler)
                        onConfigured()
                    }.onFailure { fail() }
                }

                override fun onConfigureFailed(active: CameraCaptureSession) {
                    active.close()
                    if (token == generation) fail()
                }
            }, handler)
        }.onFailure { fail() }
    }

    private fun chooseSize(sizes: Array<Size>): Size {
        val limited = sizes.filter { it.width <= 1920 && it.height <= 1080 }
        return limited.minWithOrNull(compareBy<Size> {
            abs(it.width.toDouble() / it.height - 16.0 / 9)
        }.thenByDescending { it.width * it.height }) ?: sizes.minBy { it.width * it.height }
    }

    private fun sensorOrientation() = info?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

    private fun applyPreviewScale(size: Size) {
        if (preview.width == 0 || preview.height == 0) return
        val rotated = sensorOrientation() % 180 != 0
        val ratio = if (rotated) size.height.toFloat() / size.width else size.width.toFloat() / size.height
        val viewRatio = preview.width.toFloat() / preview.height
        preview.setTransform(Matrix().apply {
            setScale(maxOf(1f, ratio / viewRatio), maxOf(1f, viewRatio / ratio),
                preview.width / 2f, preview.height / 2f)
        })
    }

    private fun fail() {
        close()?.delete()
        onError()
    }
}
