package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Property
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.OverlayGuideActivity
import com.futurecode.voicelockscreen.activity.VoiceCameraActivity
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentCameraVoiceBinding
import com.futurecode.voicelockscreen.databinding.ItemCameraCommandBinding
import com.futurecode.voicelockscreen.service.CameraVoiceService
import com.futurecode.voicelockscreen.utils.CameraSpeech
import com.futurecode.voicelockscreen.utils.LockPermissionHelper
import com.futurecode.voicelockscreen.utils.SpeechSession
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Open Camera by Voice: permissions -> confirm voice (3 recordings) -> how it works
 * -> main ON/OFF toggle (with first-run coach mark) -> all set.
 */
class CameraVoiceFragment :
    BaseFragment<FragmentCameraVoiceBinding>(FragmentCameraVoiceBinding::inflate) {

    private enum class Step { PERMISSIONS, RECORD, HOW_IT_WORKS, MAIN }

    private var step = Step.PERMISSIONS
    private var recordings = 0
    private var listening = false
    /** Where back goes for screens opened from the main screen (help, reset, all set). */
    private var returnToMain = false

    private var speechSession: SpeechSession? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val stepAnimators = mutableListOf<ValueAnimator>()
    private val guideHandler = Handler(Looper.getMainLooper())

    private val singlePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) toast(R.string.camera_permission_denied)
        refreshPermissionSwitches()
    }

    private val recordPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) startListening() else toast(R.string.camera_permission_denied)
    }

    // region Lifecycle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreState(savedInstanceState)
        setupHeader()
        setupPermissions()
        setupRecording()
        setupHowItWorks()
        setupMain()
        setupBottomActions()
        setupBackHandling()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                CameraVoiceService.runningStatus.collect {
                    if (step == Step.MAIN) renderToggle()
                }
            }
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        syncSetupListening()
        if (step == Step.PERMISSIONS) refreshPermissionSwitches()
        if (step == Step.MAIN) renderToggle()
    }

    override fun onPause() {
        CameraVoiceService.setSetupListening(false)
        super.onPause()
    }

    private fun syncSetupListening() {
        CameraVoiceService.setSetupListening(isResumed && step == Step.MAIN)
    }

    override fun onDestroyView() {
        listening = false
        speechSession?.release()
        speechSession = null
        tts?.shutdown()
        tts = null
        ttsReady = false
        stopStepAnimations()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_STEP, step.name)
        outState.putInt(KEY_RECORDINGS, recordings)
        outState.putBoolean(KEY_RETURN_TO_MAIN, returnToMain)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            step = Step.valueOf(savedInstanceState.getString(KEY_STEP, Step.PERMISSIONS.name))
            recordings = savedInstanceState.getInt(KEY_RECORDINGS)
            returnToMain = savedInstanceState.getBoolean(KEY_RETURN_TO_MAIN)
            return
        }
        val context = requireContext()
        step = when {
            VoiceLockFeatureStore.isCameraVoiceReady(context) -> Step.MAIN
            !hasAllPermissions() -> Step.PERMISSIONS
            else -> Step.RECORD
        }
    }

    // endregion

    // region Setup

    private fun setupHeader() {
        binding.btnBack.setOnClickListener { navigateBack() }
        binding.btnHelp.setOnClickListener { openHowItWorksFromMain() }
        binding.btnCoachOk.setOnClickListener { hideCoachMark() }
        binding.coachScrim.setOnClickListener { hideCoachMark() }
    }

    private fun setupPermissions() {
        binding.cameraPermissionRow.setOnClickListener { requestCameraPermission() }
        binding.switchCamera.setOnClickListener { requestCameraPermission() }
        binding.microphonePermissionRow.setOnClickListener { requestMicrophonePermission() }
        binding.switchMicrophone.setOnClickListener { requestMicrophonePermission() }
        binding.displayPermissionRow.setOnClickListener { requestOverlayPermission() }
        binding.switchDisplay.setOnClickListener { requestOverlayPermission() }
    }

    private fun setupRecording() {
        binding.tvPrompt.text = boldText(R.string.camera_phrase_test, highlight = false)
        binding.tvRecordPrompt.text = boldText(R.string.camera_record_say, highlight = false)
        binding.btnHearPhrase.setOnClickListener { speak(R.string.camera_phrase_open) }
        binding.btnRecord.setOnClickListener { onRecordTapped() }
    }

    private fun setupHowItWorks() {
        binding.tvWakeDescription.text = boldText(R.string.camera_wake_description, highlight = true)
        binding.btnHearWake.setOnClickListener { speak(R.string.camera_phrase_open) }
        bindCommand(binding.rowCapture, R.string.camera_command_capture, R.string.camera_phrase_capture)
        bindCommand(binding.rowOpenPhoto, R.string.camera_command_open_photo, R.string.camera_phrase_open_photo)
        bindCommand(binding.rowOpenVideo, R.string.camera_command_open_video, R.string.camera_phrase_open_video)
        bindCommand(binding.rowRecordVideo, R.string.camera_command_record, R.string.camera_phrase_record)
        bindCommand(binding.rowEndVideo, R.string.camera_command_end_record, R.string.camera_phrase_end_record)
    }

    private fun bindCommand(row: ItemCameraCommandBinding, @StringRes label: Int, @StringRes phrase: Int) {
        row.tvCommand.text = boldText(label, highlight = false)
        row.btnHearCommand.setOnClickListener { speak(phrase) }
    }

    private fun setupMain() {
        binding.cameraToggle.setOnClickListener { onToggleTapped() }
        binding.btnResetVoice.setOnClickListener { resetVoice() }
        binding.btnTryVoiceCamera.setOnClickListener { startActivity(VoiceCameraActivity.newIntent(requireContext())) }
        // Keep the decorative blobs inside the artwork card's rounded corners.
    }

    private fun setupBottomActions() {
        binding.btnPrimary.setOnClickListener { onPrimaryTapped() }
    }

    private fun setupBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.coachScrim.isVisible) hideCoachMark() else navigateBack()
                }
            }
        )
    }

    // endregion

    // region Events

    private fun onPrimaryTapped() {
        when (step) {
            Step.PERMISSIONS -> continueFromPermissions()
            Step.HOW_IT_WORKS -> {
                returnToMain = false
                goTo(Step.MAIN)
            }
            else -> Unit
        }
    }

    private fun continueFromPermissions() {
        val context = requireContext()
        when {
            !hasPermission(Manifest.permission.CAMERA) -> requestCameraPermission()
            !hasPermission(Manifest.permission.RECORD_AUDIO) -> requestMicrophonePermission()
            !LockPermissionHelper.hasOverlayPermission(context) -> requestOverlayPermission()
            VoiceLockFeatureStore.isCameraVoiceReady(context) -> goTo(Step.MAIN)
            else -> goTo(Step.RECORD)
        }
    }

    private fun onRecordTapped() {
        if (listening) {
            speechSession?.stopListening()
            return
        }
        val needed = arrayOf(Manifest.permission.RECORD_AUDIO)
        if (needed.all(::hasPermission)) startListening() else recordPermissions.launch(needed)
    }

    private fun onToggleTapped() {
        val context = requireContext()
        val turnOn = !CameraVoiceService.isRunning
        if (turnOn && !CameraSpeech.hasInternet(context)) {
            toast(R.string.camera_voice_internet_required)
            return
        }
        if (turnOn && !hasAllPermissions()) {
            returnToMain = true
            goTo(Step.PERMISSIONS)
            return
        }
        VoiceLockFeatureStore.setCameraEnabled(context, turnOn)
        if (turnOn && !CameraVoiceService.start(context)) {
            VoiceLockFeatureStore.setCameraEnabled(context, false)
            toast(R.string.camera_voice_unavailable)
            renderToggle()
            return
        }
        if (!turnOn) CameraVoiceService.stop(context)
        renderToggle()
        if (turnOn) {
            binding.cameraToggle.postDelayed({
                if (bindingOrNull != null && step == Step.MAIN && CameraVoiceService.isRunning) {
                    returnToMain = true
                    showSetupSuccess(R.string.camera_all_set_title) { findNavController().popBackStack() }
                }
            }, TOGGLE_TO_COMPLETE_MS)
        }
    }

    private fun resetVoice() {
        recordings = 0
        returnToMain = true
        goTo(Step.RECORD)
    }

    private fun openHowItWorksFromMain() {
        hideCoachMark()
        returnToMain = true
        goTo(Step.HOW_IT_WORKS)
    }

    private fun navigateBack() {
        val backToMain = returnToMain && step != Step.MAIN &&
            VoiceLockFeatureStore.isCameraVoiceReady(requireContext())
        if (backToMain) {
            returnToMain = false
            goTo(Step.MAIN)
        } else {
            findNavController().popBackStack()
        }
    }

    // endregion

    // region Permissions

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED

    private fun hasAllPermissions() =
        hasPermission(Manifest.permission.CAMERA) &&
            hasPermission(Manifest.permission.RECORD_AUDIO) &&
            LockPermissionHelper.hasOverlayPermission(requireContext())

    private fun requestCameraPermission() {
        if (hasPermission(Manifest.permission.CAMERA)) refreshPermissionSwitches()
        else singlePermission.launch(Manifest.permission.CAMERA)
    }

    private fun requestMicrophonePermission() {
        if (hasPermission(Manifest.permission.RECORD_AUDIO)) refreshPermissionSwitches()
        else singlePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun requestOverlayPermission() {
        val context = requireContext()
        if (LockPermissionHelper.hasOverlayPermission(context)) {
            refreshPermissionSwitches()
            return
        }
        val activity = requireActivity()
        val opened = runCatching { activity.startActivity(LockPermissionHelper.overlaySettingsIntent(context)) }
        if (opened.isFailure) {
            refreshPermissionSwitches()
            return
        }
        // Settings forwards through a trampoline activity; launch the guide once that has
        // settled so it lands on top of the real settings page in the same task.
        guideHandler.postDelayed({
            if (!activity.isFinishing && !activity.isDestroyed) {
                activity.startActivity(Intent(activity, OverlayGuideActivity::class.java))
            }
        }, OVERLAY_GUIDE_DELAY_MS)
    }

    private fun refreshPermissionSwitches() {
        val b = bindingOrNull ?: return
        b.switchCamera.isChecked = hasPermission(Manifest.permission.CAMERA)
        b.switchMicrophone.isChecked = hasPermission(Manifest.permission.RECORD_AUDIO)
        b.switchDisplay.isChecked = LockPermissionHelper.hasOverlayPermission(requireContext())
    }

    // endregion

    // region Voice recording

    private fun startListening() {
        if (listening) return
        val context = requireContext()
        if (!CameraSpeech.hasInternet(context)) {
            toast(R.string.camera_voice_internet_required)
            return
        }
        if (!SpeechSession.isAvailable(context)) {
            toast(R.string.speech_not_available)
            return
        }
        listening = true
        renderListening(true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, CameraSpeech.COMMAND_LANGUAGE)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }
        val session = speechSession ?: SpeechSession(context).also { speechSession = it }
        session.start(intent, recognitionListener)
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onError(error: Int) {
            onRecognitionFinished(spoken = null)
        }

        override fun onResults(results: Bundle?) {
            val spoken = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull { VoiceSetupFragment.normalizePhrase(it) == CAMERA_PHRASE }
            onRecognitionFinished(spoken)
        }
    }

    private fun onRecognitionFinished(spoken: String?) {
        listening = false
        if (bindingOrNull == null || step != Step.RECORD) return
        renderListening(false)
        if (!CameraSpeech.hasInternet(requireContext())) {
            toast(R.string.camera_voice_internet_required)
            return
        }
        if (spoken == null) {
            showRecordingError()
            return
        }
        recordings += 1
        VoiceLockFeatureStore.saveCameraPhrase(requireContext(), spoken)
        if (recordings >= REQUIRED_RECORDINGS) {
            VoiceLockFeatureStore.setCameraVoiceReady(requireContext(), true)
            binding.progressRing.setProgress(1f, animate = false)
            binding.progressRing.postDelayed({
                if (bindingOrNull != null && step == Step.RECORD) goTo(Step.HOW_IT_WORKS)
            }, RECORD_DONE_MS)
        } else {
            renderRecording(animate = true)
        }
    }

    // endregion

    // region Rendering

    private fun goTo(next: Step) {
        step = next
        render()
        binding.contentScroll.scrollTo(0, 0)
    }

    private fun render() {
        syncSetupListening()
        stopStepAnimations()
        binding.clPermissions.isVisible = step == Step.PERMISSIONS
        binding.clRecording.isVisible = step == Step.RECORD
        binding.clHowItWorks.isVisible = step == Step.HOW_IT_WORKS
        binding.clMain.isVisible = step == Step.MAIN
        binding.recordPanel.isVisible = step == Step.RECORD
        binding.mainControls.isVisible = step == Step.MAIN
        binding.btnHelp.isVisible = step == Step.MAIN
        binding.btnPrimary.isVisible = step in setOf(Step.PERMISSIONS, Step.HOW_IT_WORKS)

        when (step) {
            Step.PERMISSIONS -> renderPermissions()
            Step.RECORD -> renderRecording(animate = false)
            Step.HOW_IT_WORKS -> renderHowItWorks()
            Step.MAIN -> renderMain()
        }
    }

    private fun renderPermissions() {
        setTitle(R.string.permissions_required, centered = false)
        binding.btnPrimary.setText(R.string.continue_text)
        refreshPermissionSwitches()
    }

    private fun renderRecording(animate: Boolean) {
        setTitle(R.string.confirm_your_voice, centered = false)
        val stage = recordings.coerceIn(0, REQUIRED_RECORDINGS - 1)
        binding.promptPill.isVisible = stage == 0
        val (title, description) = when (stage) {
            0 -> R.string.camera_recording_initial to R.string.camera_recording_initial_desc
            1 -> R.string.camera_recording_second to R.string.camera_recording_second_desc
            else -> R.string.camera_recording_third to R.string.camera_recording_third_desc
        }
        binding.tvRecordingTitle.setText(title)
        binding.tvRecordingDescription.setText(description)
        binding.tvRecordingDescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        binding.progressRing.setProgress((stage + 1f) / REQUIRED_RECORDINGS, animate = false)
        renderListening(listening)
    }

    /** Error replaces the stage description so the record panel never shifts. */
    private fun showRecordingError() {
        binding.tvRecordingDescription.setText(R.string.we_couldnt_hear)
        binding.tvRecordingDescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
    }

    private fun renderListening(active: Boolean) {
        if (active) renderRecordingDescriptionOnly()
        binding.tvRecordHint.setText(if (active) R.string.recording_listening else R.string.tap_to_record)
    }

    private fun renderRecordingDescriptionOnly() {
        val description = when (recordings.coerceIn(0, REQUIRED_RECORDINGS - 1)) {
            0 -> R.string.camera_recording_initial_desc
            1 -> R.string.camera_recording_second_desc
            else -> R.string.camera_recording_third_desc
        }
        binding.tvRecordingDescription.setText(description)
        binding.tvRecordingDescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
    }

    private fun renderHowItWorks() {
        setTitle(R.string.camera_how_it_works, centered = true)
        binding.btnPrimary.setText(R.string.continue_text)
    }

    private fun renderMain() {
        setTitle(null, centered = false)
        renderToggle()
        // The pointing hand is the only animation on this screen (as in the reference).
        val density = resources.displayMetrics.density
        stepAnimators += repeating(binding.ivTapHand, View.TRANSLATION_Y, 0f, -10f * density, TAP_MS)
        if (!VoiceLockFeatureStore.isCameraCoachShown(requireContext()) && !binding.coachScrim.isVisible) {
            binding.btnHelp.post { if (bindingOrNull != null && step == Step.MAIN) showCoachMark() }
        }
    }

    private fun renderToggle() {
        val on = CameraVoiceService.isRunning && VoiceLockFeatureStore.isCameraEnabled(requireContext())
        val knobParams = binding.toggleKnob.layoutParams as ConstraintLayout.LayoutParams
        knobParams.horizontalBias = if (on) 1f else 0f
        binding.toggleKnob.layoutParams = knobParams
        val labelParams = binding.tvToggleLabel.layoutParams as ConstraintLayout.LayoutParams
        labelParams.horizontalBias = if (on) 0f else 1f
        binding.tvToggleLabel.layoutParams = labelParams

        binding.cameraToggle.setBackgroundResource(if (on) R.drawable.bg_camera_toggle_on else R.drawable.bg_camera_toggle_off)
        binding.toggleKnob.setBackgroundResource(if (on) R.drawable.bg_camera_toggle_knob_on else R.drawable.bg_camera_toggle_knob_off)
        binding.tvToggleLabel.setText(if (on) R.string.camera_toggle_on else R.string.camera_toggle_off)
        binding.tvToggleLabel.setTextColor(
            ContextCompat.getColor(requireContext(), if (on) R.color.white else R.color.primary_text)
        )
        binding.cameraToggle.contentDescription =
            "${getString(R.string.open_camera_by_voice)}, ${binding.tvToggleLabel.text}"
        binding.ivTapHand.visibility = if (on) View.INVISIBLE else View.VISIBLE
    }

    private fun setTitle(@StringRes title: Int?, centered: Boolean) {
        binding.tvTitle.isVisible = title != null
        if (title == null) return
        binding.tvTitle.setText(title)
        val params = binding.tvTitle.layoutParams as ConstraintLayout.LayoutParams
        if (centered) {
            // Centre on the screen: mirror the back button's footprint on the end side.
            val inset = resources.getDimensionPixelSize(R.dimen.spacing_lg) + binding.btnBack.layoutParams.width
            params.startToEnd = ConstraintLayout.LayoutParams.UNSET
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.marginStart = inset
            params.marginEnd = inset
        } else {
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.startToEnd = binding.btnBack.id
            params.marginStart = resources.getDimensionPixelSize(R.dimen.spacing_md)
            params.marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_lg)
        }
        binding.tvTitle.layoutParams = params
        binding.tvTitle.gravity = if (centered) android.view.Gravity.CENTER else android.view.Gravity.START
    }

    // endregion

    // region Coach mark

    private fun showCoachMark() {
        val elevated = resources.displayMetrics.density * COACH_TARGET_ELEVATION_DP
        binding.btnHelp.elevation = elevated
        binding.coachScrim.isVisible = true
        binding.coachArrow.isVisible = true
        binding.coachTooltip.isVisible = true
    }

    private fun hideCoachMark() {
        if (!binding.coachScrim.isVisible) return
        VoiceLockFeatureStore.setCameraCoachShown(requireContext())
        binding.coachScrim.isVisible = false
        binding.coachArrow.isVisible = false
        binding.coachTooltip.isVisible = false
        binding.btnHelp.elevation = 0f
    }

    // endregion

    // region Helpers

    /** Renders `<b>` markup; highlighted bold runs also use the brand colour. */
    private fun boldText(@StringRes res: Int, highlight: Boolean): CharSequence {
        val html = HtmlCompat.fromHtml(getString(res), HtmlCompat.FROM_HTML_MODE_LEGACY)
        val spannable = SpannableString(html)
        val primary = ContextCompat.getColor(requireContext(), R.color.primary)
        val ink = ContextCompat.getColor(requireContext(), R.color.primary_text)
        spannable.getSpans(0, spannable.length, StyleSpan::class.java)
            .filter { it.style == Typeface.BOLD }
            .forEach { span ->
                spannable.setSpan(
                    ForegroundColorSpan(if (highlight) primary else ink),
                    spannable.getSpanStart(span),
                    spannable.getSpanEnd(span),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        return spannable
    }

    private fun speak(@StringRes phrase: Int) {
        val text = getString(phrase)
        val engine = tts
        if (engine != null && ttsReady) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE)
            return
        }
        if (engine != null) return
        tts = TextToSpeech(requireContext().applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.US
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE)
            }
        }
    }

    private fun repeating(
        target: View,
        property: Property<View, Float>,
        from: Float,
        to: Float,
        durationMs: Long,
        reverse: Boolean = true
    ): ObjectAnimator = ObjectAnimator.ofFloat(target, property, from, to).apply {
        duration = durationMs
        repeatCount = ValueAnimator.INFINITE
        repeatMode = if (reverse) ValueAnimator.REVERSE else ValueAnimator.RESTART
        if (reverse) start()
    }

    private fun stopStepAnimations() {
        stepAnimators.forEach { it.cancel() }
        stepAnimators.clear()
    }

    private fun toast(@StringRes message: Int) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // endregion

    companion object {
        private const val KEY_STEP = "step"
        private const val KEY_RECORDINGS = "recordings"
        private const val KEY_RETURN_TO_MAIN = "return_to_main"
        private const val CAMERA_PHRASE = "open camera"
        private const val REQUIRED_RECORDINGS = 3
        private const val TTS_UTTERANCE = "camera_phrase"

        private const val RECORD_DONE_MS = 700L
        private const val TOGGLE_TO_COMPLETE_MS = 450L
        private const val TAP_MS = 550L
        private const val COACH_TARGET_ELEVATION_DP = 16f
        private const val OVERLAY_GUIDE_DELAY_MS = 700L

    }
}
