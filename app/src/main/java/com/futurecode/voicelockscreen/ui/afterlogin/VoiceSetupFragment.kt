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
import com.futurecode.voicelockscreen.utils.VoiceLanguagePicker
import java.util.Locale

/**
 * Voice password setup (reference copy). Record screen -> "Your Voice Key" -> confirm the phrase twice,
 * then the existing security-question step. Saving still goes through [SecureVoiceStore].
 */
class VoiceSetupFragment : BaseFragment<FragmentVoiceSetupBinding>(FragmentVoiceSetupBinding::inflate) {

    private enum class Page { RECORD, RESULT, CONFIRM }

    private var page = Page.RECORD
    private var phrase = ""
    private var confirmStep = 1
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
        page = savedInstanceState?.getString(KEY_PAGE)?.let { Page.valueOf(it) } ?: Page.RECORD
        phrase = savedInstanceState?.getString(KEY_PHRASE).orEmpty()
        confirmStep = savedInstanceState?.getInt(KEY_STEP) ?: 1
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
        }
        with(binding.result) {
            ivBack.setOnClickListener { goBack() }
            btnRecordAgain.setOnClickListener { phrase = ""; show(Page.RECORD) }
            btnContinue.setOnClickListener { confirmStep = 1; show(Page.CONFIRM) }
        }
        with(binding.confirm) {
            ivBack.setOnClickListener { goBack() }
            buttonRecord.setOnClickListener { if (listening) stopListening() else requestListening() }
        }
        show(page)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PAGE, page.name)
        outState.putString(KEY_PHRASE, phrase)
        outState.putInt(KEY_STEP, confirmStep)
    }

    private fun goBack() {
        if (listening) stopListening()
        when (page) {
            Page.RECORD -> findNavController().popBackStack()
            Page.RESULT -> { phrase = ""; show(Page.RECORD) }
            Page.CONFIRM -> show(Page.RESULT)
        }
    }

    private fun show(target: Page) {
        page = target
        back.isEnabled = target != Page.RECORD
        val b = bindingOrNull ?: return
        b.record.root.isVisible = target == Page.RECORD
        b.result.root.isVisible = target == Page.RESULT
        b.confirm.root.isVisible = target == Page.CONFIRM
        when (target) {
            Page.RECORD -> renderRecord(warning = false)
            Page.RESULT -> b.result.tvRecorded.text = phrase
            Page.CONFIRM -> renderConfirm(error = null)
        }
    }

    private fun renderRecord(warning: Boolean) {
        val r = bindingOrNull?.record ?: return
        r.tvRecorded.text = if (listening) r.tvRecorded.text else ""
        val empty = r.tvRecorded.text.isNullOrBlank()
        r.llChooseLanguage.isVisible = empty && !listening
        r.tvExplainLanguage.isVisible = empty && !listening
        r.tvWarning.isInvisible = !warning
        r.ivRecord.isVisible = !listening
        r.ivRecording.isVisible = listening
        r.animRecording.isVisible = listening
        r.progressBar.isVisible = listening
        if (listening) r.animRecording.playAnimation() else r.animRecording.cancelAnimation()
    }

    private fun renderConfirm(error: Int?) {
        val c = bindingOrNull?.confirm ?: return
        c.imageState.setImageResource(
            if (confirmStep == 1) R.drawable.vn_ic_confirm_voice_lock_step_1 else R.drawable.vn_ic_confirm_voice_lock_step_2
        )
        c.textState.setText(R.string.vn_text_confirm_your_password)
        c.textDesc.setText(
            if (confirmStep == 1) R.string.vn_text_confirm_voice_lock_repeat_password_desc
            else R.string.vn_text_confirm_voice_lock_last_password_desc
        )
        c.textError.isVisible = error != null
        error?.let { c.textError.setText(it) }
        c.imageVoice.isVisible = !listening
        c.animRecording.isVisible = listening
        c.progressBar.isVisible = listening
        if (listening) c.animRecording.playAnimation() else c.animRecording.cancelAnimation()
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
        if (page == Page.RECORD) binding.record.tvRecorded.text = ""
        refresh()
        startProgress()
        val session = speechSession ?: SpeechSession(requireContext()).also { speechSession = it }
        session.start(SpeechSession.voiceIntent(requireContext(), partialResults = true), listener)
    }

    private fun stopListening() {
        if (!listening) return
        speechSession?.stopListening()
    }

    /** Reference: a 5 s ring around the mic; anything after 5 s doesn't count. */
    private fun startProgress() {
        progressAnimator?.cancel()
        val bar = if (page == Page.RECORD) binding.record.progressBar else binding.confirm.progressBar
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

    private fun refresh() {
        when (page) {
            Page.RECORD -> renderRecord(warning = false)
            Page.CONFIRM -> renderConfirm(error = null)
            Page.RESULT -> Unit
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            if (page != Page.RECORD) return
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
        val normalized = normalizePhrase(heard)
        when (page) {
            Page.RECORD -> {
                if (normalized.isBlank()) {
                    binding.record.tvRecorded.text = ""
                    renderRecord(warning = true)
                } else {
                    phrase = heard
                    show(Page.RESULT)
                }
            }
            Page.CONFIRM -> when {
                normalized.isBlank() -> renderConfirm(R.string.vn_text_warning_no_voice_detected)
                normalized != normalizePhrase(phrase) -> renderConfirm(R.string.vn_text_wrong_password_please_try_again)
                confirmStep == 1 -> { confirmStep = 2; renderConfirm(null) }
                else -> {
                    SecureVoiceStore.savePhrase(requireContext(), phrase)
                    findNavController().navigate(R.id.action_voiceSetupFragment_to_securityQuestionFragment, null, VoiceNoteUi.slideNavOptions)
                }
            }
            Page.RESULT -> Unit
        }
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
        private const val KEY_PAGE = "page"
        private const val KEY_PHRASE = "phrase"
        private const val KEY_STEP = "confirm_step"
        private const val MAX_RECORD_MS = 5000L
        const val VOICE_PREFS = "voice_setup_prefs"
        const val KEY_VOICE_PHRASE = "voice_phrase"
        const val KEY_VOICE_PHRASE_NORMALIZED = "voice_phrase_normalized"
        const val KEY_VOICE_RECORDED = "voice_recorded"
        fun normalizePhrase(phrase: String): String = phrase.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{M}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
    }
}
