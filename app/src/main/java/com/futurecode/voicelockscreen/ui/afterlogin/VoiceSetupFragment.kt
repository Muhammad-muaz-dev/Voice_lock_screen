package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentVoiceSetupBinding
import com.futurecode.voicelockscreen.service.LockMonitorService
import java.util.Locale

class VoiceSetupFragment : BaseFragment<FragmentVoiceSetupBinding>(FragmentVoiceSetupBinding::inflate) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizedPhrase: String = ""
    private var isListening = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recognizedPhrase = loadSavedPhrase()
        renderRecordedState()

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnRecord.setOnClickListener { startRecording() }
        binding.btnRecordAgain.setOnClickListener { startRecording() }
        binding.btnContinue.setOnClickListener {
            if (saveVoicePhrase()) {
                LockMonitorService.startIfNeeded(requireContext())
                Toast.makeText(requireContext(), R.string.voice_saved, Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_voiceSetupFragment_to_securityQuestionFragment)
            }
        }
    }

    override fun onDestroyView() {
        stopRecognizer()
        super.onDestroyView()
    }

    private fun startRecording() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), R.string.speech_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        stopRecognizer()
        recognizedPhrase = ""
        isListening = true
        renderListeningState()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    binding.tvRecorded.text = getString(R.string.recording_listening)
                    binding.tvTapHint.text = getString(R.string.speak_secret_phrase)
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    isListening = false
                    binding.tvTimer.text = "0:04"
                }

                override fun onError(error: Int) {
                    isListening = false
                    recognizedPhrase = ""
                    renderEmptyState()
                    Toast.makeText(requireContext(), R.string.voice_not_detected, Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    recognizedPhrase = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                        .trim()

                    if (recognizedPhrase.isBlank()) {
                        renderEmptyState()
                        Toast.makeText(requireContext(), R.string.voice_not_detected, Toast.LENGTH_SHORT).show()
                    } else {
                        renderRecordedState()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) binding.tvPhrase.text = partial
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            startListening(speechIntent())
        }
    }

    private fun renderListeningState() {
        binding.tvRecorded.text = getString(R.string.recording_listening)
        binding.tvTimer.text = "0:00"
        binding.tvPhrase.text = getString(R.string.speak_secret_phrase)
        binding.tvTapHint.text = getString(R.string.speak_secret_phrase)
        binding.btnContinue.isEnabled = false
        binding.btnContinue.alpha = 0.65f
    }

    private fun renderRecordedState() {
        if (recognizedPhrase.isBlank()) {
            renderEmptyState()
            return
        }

        binding.tvRecorded.text = getString(R.string.recorded_successfully)
        binding.tvTimer.text = "0:04"
        binding.tvPhrase.text = recognizedPhrase
        binding.tvTapHint.text = getString(R.string.tap_to_record_again)
        binding.btnContinue.isEnabled = true
        binding.btnContinue.alpha = 1f
    }

    private fun renderEmptyState() {
        binding.tvRecorded.text = getString(R.string.record_voice)
        binding.tvTimer.text = "0:00"
        binding.tvPhrase.text = ""
        binding.tvTapHint.text = getString(R.string.speak_secret_phrase)
        binding.btnContinue.isEnabled = false
        binding.btnContinue.alpha = 0.65f
    }

    private fun saveVoicePhrase(): Boolean {
        if (recognizedPhrase.isBlank() || isListening) {
            Toast.makeText(requireContext(), R.string.voice_not_detected, Toast.LENGTH_SHORT).show()
            return false
        }

        requireContext()
            .getSharedPreferences(VOICE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VOICE_PHRASE, recognizedPhrase)
            .putString(KEY_VOICE_PHRASE_NORMALIZED, normalizePhrase(recognizedPhrase))
            .putBoolean(KEY_VOICE_RECORDED, true)
            .apply()
        return true
    }

    private fun loadSavedPhrase(): String {
        return requireContext()
            .getSharedPreferences(VOICE_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_VOICE_PHRASE, "")
            .orEmpty()
    }

    private fun stopRecognizer() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun speechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    companion object {
        const val VOICE_PREFS = "voice_setup_prefs"
        const val KEY_VOICE_PHRASE = "voice_phrase"
        const val KEY_VOICE_PHRASE_NORMALIZED = "voice_phrase_normalized"
        const val KEY_VOICE_RECORDED = "voice_recorded"

        fun normalizePhrase(phrase: String): String {
            return phrase
                .lowercase(Locale.US)
                .replace(Regex("[^a-z0-9 ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}
