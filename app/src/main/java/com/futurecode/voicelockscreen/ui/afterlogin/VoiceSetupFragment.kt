package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.ads.waterfall.AdWaterfallFullScreenAdsHelper
import com.futurecode.voicelockscreen.ads.waterfall.AdWaterfallNativeAdsHelper
import com.futurecode.voicelockscreen.ads.waterfall.AdWaterfallNativeBannerAdsHelper
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentVoiceSetupBinding
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.service.LockService
import java.util.Locale

class VoiceSetupFragment : BaseFragment<FragmentVoiceSetupBinding>(FragmentVoiceSetupBinding::inflate) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizedPhrase: String = ""
    private var isListening = false


    private var nativeAdsHelper: AdWaterfallNativeAdsHelper? = null
    private var fullScreenAdsHelper: AdWaterfallFullScreenAdsHelper? = null
    private var nativeBanner: AdWaterfallNativeBannerAdsHelper? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nativeAdsHelper= AdWaterfallNativeAdsHelper(requireActivity())
        fullScreenAdsHelper= AdWaterfallFullScreenAdsHelper(requireActivity())
        nativeBanner= AdWaterfallNativeBannerAdsHelper(requireActivity())

        recognizedPhrase = loadSavedPhrase()
        loadNativeBannerAds()

        renderRecordedState()

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnRecord.setOnClickListener { startRecording() }
        binding.btnRecordAgain.setOnClickListener { startRecording() }
        binding.btnContinue.setOnClickListener {
            if (saveVoicePhrase()) {
                LockService.startIfNeeded(requireContext())
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
                    binding.tvTimer.setText(R.string.timer_four_seconds)
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
        binding.tvTimer.setText(R.string.timer_zero)
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
        binding.tvTimer.setText(R.string.timer_four_seconds)
        binding.tvPhrase.text = recognizedPhrase
        binding.tvTapHint.text = getString(R.string.tap_to_record_again)
        binding.btnContinue.isEnabled = true
        binding.btnContinue.alpha = 1f
    }

    private fun renderEmptyState() {
        binding.tvRecorded.text = getString(R.string.record_voice)
        binding.tvTimer.setText(R.string.timer_zero)
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

        SecureVoiceStore.savePhrase(requireContext(), recognizedPhrase)
        return true
    }

    private fun loadSavedPhrase(): String {
        return SecureVoiceStore.getDisplayPhrase(requireContext())
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

    fun loadNativeBannerAds() {
        activity?.let { currentActivity ->
            if (nativeBanner == null) {
                nativeBanner = AdWaterfallNativeBannerAdsHelper(currentActivity)
            }
            nativeBanner?.showNativeBAnnerAd(
                nativeBannerAdView = binding.nativeAds3.frame,
                mainLayout = binding.nativeAds3.mainLayout,
                placeholder = binding.nativeAds3.placeholder
            )
        }
    }
}
