package com.futurecode.voicelockscreen.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

/**
 * Owns a single [SpeechRecognizer] for a screen and restarts it in place.
 *
 * Destroying a recognizer and immediately creating a new one races the system
 * unbinding the speech service; on some devices (e.g. ColorOS) the new start is
 * dropped silently and the mic never opens. Reusing one instance avoids that.
 */
class SpeechSession(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    private var lastStartAt = 0L

    fun start(intent: Intent, listener: RecognitionListener) {
        // A double tap would cancel a session before it reports anything, leaking its
        // stale callbacks into the new one; one start per interval is plenty.
        val now = SystemClock.elapsedRealtime()
        if (now - lastStartAt < MIN_RESTART_INTERVAL_MS) return
        lastStartAt = now

        val activeRecognizer = recognizer
            ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        activeRecognizer.cancel()
        activeRecognizer.setRecognitionListener(RestartSafeListener(listener))
        activeRecognizer.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.cancel()
    }

    fun release() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    /**
     * A cancelled session can still report CLIENT / NO_MATCH / SPEECH_TIMEOUT after a restart.
     * Those cannot genuinely happen before the mic opens, so they are dropped until the new
     * session is ready; real failures (audio, permission, network, language) still pass through.
     */
    private class RestartSafeListener(
        private val delegate: RecognitionListener
    ) : RecognitionListener by delegate {
        private var ready = false

        override fun onReadyForSpeech(params: Bundle?) {
            ready = true
            delegate.onReadyForSpeech(params)
        }

        override fun onError(error: Int) {
            if (!ready && error in STALE_SESSION_ERRORS) return
            delegate.onError(error)
        }
    }

    companion object {
        private const val MIN_RESTART_INTERVAL_MS = 600L
        private val STALE_SESSION_ERRORS = setOf(
            SpeechRecognizer.ERROR_CLIENT,
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        )

        fun isAvailable(context: Context): Boolean =
            SpeechRecognizer.isRecognitionAvailable(context)

        fun hasMicPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

        /** Free-form intent in the voice language the user picked during setup. */
        fun voiceIntent(context: Context, partialResults: Boolean = false): Intent {
            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, VoiceLanguagePicker.selected(context))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partialResults)
            }
        }
    }
}
