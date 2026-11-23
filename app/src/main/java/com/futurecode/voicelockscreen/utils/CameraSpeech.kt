package com.futurecode.voicelockscreen.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

enum class CameraCommand {
    OPEN, PHOTO, VIDEO, CAPTURE, RECORD, STOP;

    companion object {
        fun fromPhrase(text: String, wakePhrase: String): CameraCommand? {
            val phrase = normalize(text)
            return when (phrase) {
                "" -> null
                "open camera", normalize(wakePhrase) -> OPEN
                "photo", "open photo", "switch photo", "switch to photo", "photo mode" -> PHOTO
                "video", "open video", "switch video", "switch to video", "video mode" -> VIDEO
                "capture", "capture photo", "take photo", "take a photo", "take picture", "take a picture" -> CAPTURE
                "record", "record video", "start video", "start recording" -> RECORD
                "end record", "end recording", "stop recording", "stop video" -> STOP
                else -> null
            }
        }

        private fun normalize(text: String) = text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z ]"), " ").trim().replace(Regex("\\s+"), " ")
    }
}

/** Main-thread recognition using the default speech provider. */
class CameraSpeech(
    private val context: Context,
    private val onCommand: (CameraCommand) -> Unit,
    private val onUnavailable: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var failures = 0
    private val listen = Runnable { listenOnce() }
    private val watchdog = Runnable {
        recognizer?.cancel()
        retry(error = true)
    }

    fun start() {
        if (recognizer != null || !hasInternet(context)) return
        if (!isAvailable(context) || !SpeechSession.hasMicPermission(context)) {
            onUnavailable()
            return
        }
        val engine = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }
            .getOrElse { onUnavailable(); return }
        failures = 0
        recognizer = engine
        engine.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (recognizer === engine) Log.d(TAG, "Speech recognizer ready")
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onError(error: Int) {
                if (recognizer !== engine) return
                if (!hasInternet(context)) {
                    stop()
                    return
                }
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Log.w(TAG, "Speech recognition error: $error")
                }
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> unavailable()
                    else -> retry(error !in setOf(
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    ))
                }
            }

            override fun onResults(results: Bundle?) {
                if (recognizer !== engine) return
                if (!hasInternet(context)) {
                    stop()
                    return
                }
                val wakePhrase = VoiceLockFeatureStore.cameraPhrase(context)
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstNotNullOfOrNull { CameraCommand.fromPhrase(it, wakePhrase) }
                    ?.let(onCommand)
                retry(error = false)
            }
        })
        handler.postDelayed(listen, 350L)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        val engine = recognizer
        recognizer = null
        engine?.destroy()
    }

    private fun listenOnce() {
        val engine = recognizer ?: return
        if (!hasInternet(context)) {
            stop()
            return
        }
        runCatching { engine.startListening(commandIntent()) }.onSuccess {
            handler.postDelayed(watchdog, 15_000L)
        }.onFailure { retry(error = true) }
    }

    private fun commandIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, COMMAND_LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
    }

    private fun retry(error: Boolean) {
        handler.removeCallbacks(watchdog)
        handler.removeCallbacks(listen)
        if (recognizer == null) return
        failures = if (error) failures + 1 else 0
        if (failures == 5) unavailable()
        else handler.postDelayed(listen, if (error) failures * 1_500L else 450L)
    }

    private fun unavailable() {
        stop()
        onUnavailable()
    }

    companion object {
        private const val TAG = "CameraSpeech"
        const val COMMAND_LANGUAGE = "en-US"

        fun isAvailable(context: Context) = SpeechRecognizer.isRecognitionAvailable(context)

        fun hasInternet(context: Context): Boolean {
            val connectivity = context.getSystemService(ConnectivityManager::class.java)
            val network = connectivity.activeNetwork ?: return false
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
}
