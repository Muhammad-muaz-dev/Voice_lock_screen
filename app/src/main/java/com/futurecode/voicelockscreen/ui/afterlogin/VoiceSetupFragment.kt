package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentVoiceSetupBinding
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.utils.DarkDialogs
import com.futurecode.voicelockscreen.utils.VoiceLanguagePicker
import java.util.Locale

class VoiceSetupFragment : BaseFragment<FragmentVoiceSetupBinding>(FragmentVoiceSetupBinding::inflate) {
    private var recognizer: SpeechRecognizer? = null
    private var firstPhrase = ""
    private var phrase = ""
    private var confirming = false
    private var confirmed = false
    private var listening = false
    private var startedAt = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (!listening || bindingOrNull == null) return
            val elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1000
            binding.tvTimer.text = "0:0${elapsed.coerceAtMost(5)} / 0:05"
            if (elapsed >= 5) recognizer?.stopListening() else handler.postDelayed(this, 250)
        }
    }
    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) record() else showBlocked()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firstPhrase = savedInstanceState?.getString("first").orEmpty()
        phrase = savedInstanceState?.getString("phrase").orEmpty()
        confirming = savedInstanceState?.getBoolean("confirming") ?: false
        confirmed = savedInstanceState?.getBoolean("confirmed") ?: false
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnLanguage.text = VoiceLanguagePicker.label(requireContext())
        binding.btnLanguage.setOnClickListener {
            VoiceLanguagePicker.show(requireContext()) { binding.btnLanguage.text = VoiceLanguagePicker.label(requireContext()) }
        }
        binding.btnRecord.setOnClickListener { requestRecord() }
        binding.btnRecordAgain.setOnClickListener {
            confirming = false; confirmed = false; firstPhrase = ""; phrase = ""
            requestRecord()
        }
        binding.btnContinue.setOnClickListener {
            if (confirmed) {
                SecureVoiceStore.savePhrase(requireContext(), firstPhrase)
                findNavController().navigate(R.id.action_voiceSetupFragment_to_securityQuestionFragment)
            } else if (phrase.isNotBlank()) {
                firstPhrase = phrase; phrase = ""; confirming = true
                render()
            }
        }
        render()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("first", firstPhrase); outState.putString("phrase", phrase)
        outState.putBoolean("confirming", confirming); outState.putBoolean("confirmed", confirmed)
    }
    private fun requestRecord() {
        if (listening) return
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) record()
        else DarkDialogs.show(requireContext(), getString(R.string.microphone_request_title),
            getString(R.string.microphone_request_body), getString(R.string.not_now), getString(R.string.allow)) {
            microphone.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    private fun showBlocked() {
        DarkDialogs.show(requireContext(), getString(R.string.microphone_blocked_title),
            getString(R.string.microphone_blocked_body), getString(R.string.not_now), getString(R.string.open_app_settings)) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${requireContext().packageName}")))
        }
    }
    private fun record() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) { showError(R.string.speech_not_available); return }
        recognizer?.destroy()
        listening = true; confirmed = false; phrase = ""
        startedAt = SystemClock.elapsedRealtime()
        render()
        handler.post(tick)
        recognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) {
                    listening = false; handler.removeCallbacks(tick)
                    if (bindingOrNull != null) showError(if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) R.string.microphone_blocked_body else R.string.we_couldnt_hear)
                }
                override fun onResults(results: Bundle?) {
                    listening = false; handler.removeCallbacks(tick)
                    if (bindingOrNull == null) return
                    phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
                    if (normalizePhrase(phrase).isBlank()) { showError(R.string.we_couldnt_hear); return }
                    if (confirming && normalizePhrase(phrase) != normalizePhrase(firstPhrase)) {
                        phrase = ""; showError(R.string.voice_mismatch); return
                    }
                    confirmed = confirming
                    render()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    bindingOrNull?.tvPhrase?.text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, VoiceLanguagePicker.selected(requireContext()))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
        }
    }
    private fun showError(message: Int) {
        render()
        binding.tvTapHint.setText(message)
        binding.tvTapHint.setTextColor(requireContext().getColor(R.color.errorSoft))
    }
    private fun render() {
        binding.tvTitle.setText(if (confirming && !confirmed) R.string.voice_confirm_title else if (phrase.isNotBlank()) R.string.voice_review_title else R.string.voice_record_title)
        binding.tvSubtitle.setText(if (confirming) R.string.voice_confirm_hint else R.string.voice_record_description)
        binding.tvRecorded.setText(if (listening) R.string.recording_listening else if (phrase.isNotBlank()) R.string.voice_recorded_short else R.string.record_voice)
        binding.tvPhrase.text = phrase.ifBlank { getString(R.string.voice_phrase_placeholder) }
        binding.tvTapHint.setText(if (listening) R.string.recording_listening else R.string.tap_to_record)
        binding.tvTapHint.setTextColor(requireContext().getColor(R.color.textPrimary))
        binding.btnContinue.isEnabled = phrase.isNotBlank() && !listening
        binding.btnContinue.alpha = if (binding.btnContinue.isEnabled) 1f else .45f
        binding.btnRecordAgain.isVisible = phrase.isNotBlank()
        binding.btnLanguage.isEnabled = !listening
    }
    override fun onDestroyView() {
        listening = false; handler.removeCallbacksAndMessages(null)
        recognizer?.cancel(); recognizer?.destroy(); recognizer = null
        super.onDestroyView()
    }
    companion object {
        const val VOICE_PREFS = "voice_setup_prefs"
        const val KEY_VOICE_PHRASE = "voice_phrase"
        const val KEY_VOICE_PHRASE_NORMALIZED = "voice_phrase_normalized"
        const val KEY_VOICE_RECORDED = "voice_recorded"
        fun normalizePhrase(phrase: String): String = phrase.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{M}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
    }
}
