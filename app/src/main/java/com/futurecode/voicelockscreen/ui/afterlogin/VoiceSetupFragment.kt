package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.animation.ValueAnimator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentVoiceSetupBinding
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.utils.SpeechSession
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore
import com.futurecode.voicelockscreen.utils.VoiceLanguagePicker
import java.util.Locale

/** Records the voice password. The recovery question is its own next step. */
class VoiceSetupFragment : BaseFragment<FragmentVoiceSetupBinding>(FragmentVoiceSetupBinding::inflate) {

    private var phrase = ""
    private var listening = false
    private var speechSession: SpeechSession? = null
    private var progressAnimator: ValueAnimator? = null

    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val context = context ?: return@registerForActivityResult
        if (granted) startListening() else VoiceNoteUi.errorToast(context, R.string.vn_microphone_permission_denied)
    }

    private val back = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = goBack()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VoiceNoteUi.lightStatusBar(this)
        phrase = savedInstanceState?.getString(KEY_PHRASE).orEmpty()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, back)

        with(binding.record) {
            ivBack.setOnClickListener { goBack() }
            tvSpeechLanguage.text = VoiceLanguagePicker.label(requireContext())
            llChooseLanguage.setOnClickListener {
                if (listening) return@setOnClickListener
                VoiceNoteUi.showSpeechLanguages(requireContext()) {
                    bindingOrNull?.record?.tvSpeechLanguage?.text = VoiceLanguagePicker.label(requireContext())
                }
            }
            ivRecord.setOnClickListener { requestListening() }
            ivRecording.setOnClickListener { stopListening() }
            btnContinue.setOnClickListener { saveVoiceKey() }
        }
        renderRecord(warning = false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PHRASE, phrase)
    }

    private fun goBack() {
        if (listening) {
            speechSession?.cancel()
            finishListening()
        }
        findNavController().popBackStack()
    }

    private fun renderRecord(warning: Boolean) {
        val r = bindingOrNull?.record ?: return
        r.tvRecorded.text = if (listening) r.tvRecorded.text else phrase
        val empty = r.tvRecorded.text.isNullOrBlank()
        r.llChooseLanguage.isVisible = empty && !listening
        r.tvExplainLanguage.isVisible = empty && !listening
        r.tvWarning.isInvisible = !warning
        r.ivRecord.isInvisible = listening
        r.ivRecording.isVisible = listening
        r.animRecording.isVisible = listening
        r.progressBar.isVisible = listening
        r.btnContinue.isEnabled = !listening && normalizePhrase(phrase).isNotBlank()
        r.btnContinue.alpha = if (r.btnContinue.isEnabled) 1f else 0.5f
        back.isEnabled = listening
        if (listening) r.animRecording.playAnimation() else r.animRecording.cancelAnimation()
    }

    private fun requestListening() {
        if (listening) return
        if (SpeechSession.hasMicPermission(requireContext())) {
            startListening()
        } else {
            VoiceNoteUi.showMicAccess(requireContext(), onOk = {
                if (isAdded) microphone.launch(Manifest.permission.RECORD_AUDIO)
            })
        }
    }

    private fun startListening() {
        if (bindingOrNull == null || listening) return
        if (!SpeechSession.isAvailable(requireContext())) {
            VoiceNoteUi.errorToast(requireContext(), R.string.vn_speech_not_available)
            return
        }
        listening = true
        phrase = ""
        binding.record.tvRecorded.text = ""
        refresh()
        startProgress()
        val session = speechSession ?: SpeechSession(requireContext()).also { speechSession = it }
        session.start(SpeechSession.voiceIntent(requireContext(), partialResults = true), listener)
    }

    private fun stopListening() {
        if (!listening) return
        speechSession?.stopListening()
    }

    /** Keep recordings to five seconds so the setup stays quick. */
    private fun startProgress() {
        progressAnimator?.cancel()
        val bar = binding.record.progressBar
        bar.max = 100
        bar.progress = 0
        progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = MAX_RECORD_MS
            interpolator = LinearInterpolator()
            addUpdateListener { bar.progress = it.animatedValue as Int }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (listening && bar.progress >= 100) stopListening()
                }
            })
            start()
        }
    }

    private fun finishListening() {
        listening = false
        progressAnimator?.cancel()
        progressAnimator = null
    }

    private fun refresh() = renderRecord(warning = false)

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            bindingOrNull?.record?.tvRecorded?.text = text
        }

        override fun onResults(results: Bundle?) {
            finishListening()
            if (bindingOrNull == null) return
            val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
            handleHeard(heard)
        }

        override fun onError(error: Int) {
            finishListening()
            val context = context ?: return
            if (bindingOrNull == null) return
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                VoiceNoteUi.errorToast(context, R.string.vn_microphone_permission_denied)
                refresh()
                return
            }
            handleHeard("")
        }
    }

    private fun handleHeard(heard: String) {
        val record = bindingOrNull?.record ?: return
        if (normalizePhrase(heard).isBlank()) {
            phrase = ""
            record.tvRecorded.text = ""
            renderRecord(warning = true)
            return
        }

        phrase = heard.trim()
        record.tvRecorded.text = phrase
        renderRecord(warning = false)
    }

    private fun saveVoiceKey() {
        val phraseToSave = phrase.trim()
        if (listening || normalizePhrase(phraseToSave).isBlank()) return
        SecureVoiceStore.savePhrase(requireContext(), phraseToSave)
        navigateAfterVoice()
    }

    /**
     * Voice -> recovery question (first time only) -> permissions this flow still needs -> done.
     * Re-recording later skips the question when one is already saved.
     */
    private fun navigateAfterVoice() {
        val context = requireContext()
        val hasQuestion = context.getSharedPreferences(VOICE_PREFS, 0).getBoolean(KEY_SECURITY_READY, false)
        val destination = when {
            !hasQuestion -> R.id.securityQuestionFragment
            VoiceLockSwitchStore.hasRequiredPermissions(context) -> return completeVoiceSetup()
            else -> R.id.permissionFragment
        }
        val args = if (destination == R.id.permissionFragment) {
            Bundle().apply { putString(PermissionFragment.ARG_TARGET, PermissionFragment.TARGET_COMPLETE) }
        } else {
            null
        }
        findNavController().navigate(destination, args, VoiceNoteUi.slideNavOptions)
    }

    override fun onPause() {
        if (listening) {
            speechSession?.cancel()
            finishListening()
            refresh()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        finishListening()
        speechSession?.release()
        speechSession = null
        super.onDestroyView()
    }

    companion object {
        private const val KEY_PHRASE = "phrase"
        private const val MAX_RECORD_MS = 5000L
        const val VOICE_PREFS = "voice_setup_prefs"
        const val KEY_VOICE_PHRASE = "voice_phrase"
        const val KEY_VOICE_PHRASE_NORMALIZED = "voice_phrase_normalized"
        const val KEY_VOICE_RECORDED = "voice_recorded"
        const val KEY_SECURITY_READY = "security_ready"
        fun normalizePhrase(phrase: String): String = phrase.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{M}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
    }
}
