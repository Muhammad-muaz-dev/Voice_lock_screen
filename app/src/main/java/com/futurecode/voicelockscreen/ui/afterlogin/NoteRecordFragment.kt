package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentNoteRecordBinding
import com.futurecode.voicelockscreen.utils.SpeechSession
import com.futurecode.voicelockscreen.utils.VoiceLanguagePicker
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.utils.VoiceNoteItem
import java.util.Locale

/** New Note / record screen (reference copy): live speech-to-text, Continue appends, Save names the note. */
class NoteRecordFragment : BaseFragment<FragmentNoteRecordBinding>(FragmentNoteRecordBinding::inflate) {

    private enum class State { IDLE, RECORDING, STOPPED }

    private var state = State.IDLE
    private var editing = false
    private var saving = false
    private var committedText = ""
    private var accumulatedMs = 0L
    private var startedAt = 0L
    private var lastVoiceAt = 0L
    private var speechSession: SpeechSession? = null
    private val handler = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            if (state != State.RECORDING || bindingOrNull == null) return
            renderTimer()
            if (SystemClock.elapsedRealtime() - lastVoiceAt >= NO_VOICE_TIMEOUT_MS) {
                binding.tvNoVoiceDetected.isVisible = true
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val restartListening = Runnable { if (state == State.RECORDING) listen() }

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val context = context ?: return@registerForActivityResult
        if (granted) startRecording() else VoiceNoteUi.errorToast(context, R.string.vn_microphone_permission_denied)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VoiceNoteUi.lightStatusBar(this)
        binding.tvSpeechLanguage.text = VoiceLanguagePicker.label(requireContext())
        setupActions()
        render()
    }

    private fun setupActions() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.llChooseLanguage.setOnClickListener {
            VoiceNoteUi.showSpeechLanguages(requireContext()) {
                bindingOrNull?.tvSpeechLanguage?.text = VoiceLanguagePicker.label(requireContext())
            }
        }
        binding.tvRecord.setOnClickListener {
            if (state == State.RECORDING) stopRecording() else requestStart()
        }
        binding.tvContinue.setOnClickListener {
            finishEditing()
            requestStart()
        }
        binding.tvSave.setOnClickListener {
            finishEditing()
            askNameAndSave()
        }
        binding.ivEdit.setOnClickListener { startEditing() }
        binding.tvDone.setOnClickListener { finishEditing() }
    }

    private fun requestStart() {
        if (!prefManager.isPro && VoiceLockFeatureStore.remainingTries(requireContext()) <= 0) {
            findNavController().navigate(R.id.premiumFragment)
            return
        }
        if (SpeechSession.hasMicPermission(requireContext())) {
            startRecording()
        } else {
            VoiceNoteUi.showMicAccess(requireContext(), descriptionRes = R.string.vn_text_note_microphone_permission_desc, onOk = {
                if (isAdded) permission.launch(Manifest.permission.RECORD_AUDIO)
            })
        }
    }

    private fun startRecording() {
        if (bindingOrNull == null) return
        if (!SpeechSession.isAvailable(requireContext())) {
            VoiceNoteUi.errorToast(requireContext(), R.string.vn_speech_not_available)
            return
        }
        if (state == State.IDLE) binding.tvNoVoiceDetected.isVisible = false
        state = State.RECORDING
        startedAt = SystemClock.elapsedRealtime()
        lastVoiceAt = startedAt
        render()
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, TICK_MS)
        listen()
    }

    private fun listen() {
        val context = context ?: return
        val session = speechSession ?: SpeechSession(context).also { speechSession = it }
        session.start(SpeechSession.voiceIntent(context, partialResults = true), listener)
    }

    private fun stopRecording() {
        if (state != State.RECORDING) return
        accumulatedMs += SystemClock.elapsedRealtime() - startedAt
        state = State.STOPPED
        handler.removeCallbacks(tick)
        handler.removeCallbacks(restartListening)
        speechSession?.stopListening()
        renderTimer()
        render()
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = markVoice()
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            if (state != State.RECORDING || bindingOrNull == null) return
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (partial.isNullOrBlank()) return
            markVoice()
            showText(join(committedText, partial))
        }

        override fun onResults(results: Bundle?) {
            if (bindingOrNull == null) return
            val final = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
            if (final.isNotEmpty()) {
                markVoice()
                appendText(final)
            } else {
                showText(committedText)
            }
            if (state == State.RECORDING) scheduleRestart() else render()
        }

        override fun onError(error: Int) {
            if (bindingOrNull == null) return
            showText(committedText)
            when {
                state != State.RECORDING -> render()
                error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> scheduleRestart()
                error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    stopRecording()
                    VoiceNoteUi.errorToast(requireContext(), R.string.vn_microphone_permission_denied)
                }
                else -> scheduleRestart()
            }
        }
    }

    private fun markVoice() {
        lastVoiceAt = SystemClock.elapsedRealtime()
        bindingOrNull?.tvNoVoiceDetected?.isVisible = false
    }

    private fun scheduleRestart() {
        handler.removeCallbacks(restartListening)
        handler.postDelayed(restartListening, RESTART_DELAY_MS)
    }

    private fun appendText(text: String) {
        val combined = join(committedText, text)
        if (VoiceNoteUi.wordCount(combined) > MAX_WORDS) {
            committedText = VoiceNoteUi.limitWords(combined, MAX_WORDS)
            showText(committedText)
            stopRecording()
            VoiceNoteUi.errorToast(requireContext(), R.string.vn_word_limit_reached)
            return
        }
        committedText = combined
        showText(committedText)
    }

    private fun join(base: String, addition: String): String =
        if (base.isBlank()) addition.trim() else "${base.trimEnd()} ${addition.trim()}"

    private fun showText(text: String) {
        val field = bindingOrNull?.etRecorded ?: return
        if (field.text?.toString() != text) field.setText(text)
        if (state != State.STOPPED) render()
    }

    private fun startEditing() {
        editing = true
        binding.etRecorded.isEnabled = true
        binding.etRecorded.setSelection(binding.etRecorded.text?.length ?: 0)
        VoiceNoteUi.showKeyboard(binding.etRecorded)
        render()
    }

    private fun finishEditing() {
        if (!editing) return
        editing = false
        committedText = binding.etRecorded.text?.toString()?.trim().orEmpty()
        binding.etRecorded.isEnabled = false
        VoiceNoteUi.hideKeyboard(binding.etRecorded)
        if (committedText.isBlank()) resetToIdle() else render()
    }

    private fun resetToIdle() {
        state = State.IDLE
        accumulatedMs = 0L
        renderTimer()
        render()
    }

    private fun askNameAndSave() {
        if (committedText.isBlank() || saving) return
        VoiceNoteUi.showNoteName(requireContext(), getString(R.string.vn_new_note)) { name -> saveNote(name) }
    }

    private fun saveNote(name: String) {
        val context = context ?: return
        if (saving) return
        saving = true
        val now = System.currentTimeMillis()
        val seconds = (accumulatedMs / 1000).toInt().coerceAtLeast(1)
        VoiceLockFeatureStore.saveNote(context, VoiceNoteItem(now, name, committedText, "", now, seconds))
        if (!prefManager.isPro) VoiceLockFeatureStore.consumeTry(context)
        VoiceNoteUi.toast(context, R.string.vn_save_note_successfully)
        if (bindingOrNull != null) findNavController().popBackStack()
    }

    private fun renderTimer() {
        val running = if (state == State.RECORDING) SystemClock.elapsedRealtime() - startedAt else 0L
        val seconds = (accumulatedMs + running) / 1000
        bindingOrNull?.tvRecordTime?.text = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
    }

    private fun render() {
        val b = bindingOrNull ?: return
        if (state == State.STOPPED && committedText.isBlank()) {
            state = State.IDLE
            accumulatedMs = 0L
            renderTimer()
        }
        val hasText = !b.etRecorded.text.isNullOrBlank()
        val recording = state == State.RECORDING
        val stopped = state == State.STOPPED
        b.llChooseLanguage.isVisible = state == State.IDLE && !hasText
        b.tvChooseLanguageDesc.isVisible = b.llChooseLanguage.isVisible
        b.tvRecord.isVisible = !stopped
        b.tvRecord.setText(if (recording) R.string.vn_stop else R.string.vn_start)
        VoiceNoteUi.setBackgroundKeepPadding(
            b.tvRecord,
            if (recording) R.drawable.vn_bg_radius_1000_warning else R.drawable.vn_bg_radius_1000_primary01
        )
        b.tvContinue.isVisible = stopped
        b.tvSave.isVisible = stopped
        b.ivEdit.isVisible = stopped && !editing
        b.tvDone.isVisible = stopped && editing
    }

    override fun onPause() {
        // Release the mic when the screen leaves the foreground; text is kept.
        stopRecording()
        super.onPause()
    }

    override fun onDestroyView() {
        state = State.IDLE
        handler.removeCallbacksAndMessages(null)
        speechSession?.release()
        speechSession = null
        super.onDestroyView()
    }

    private companion object {
        const val TICK_MS = 1000L
        const val NO_VOICE_TIMEOUT_MS = 3000L
        const val RESTART_DELAY_MS = 650L
        const val MAX_WORDS = 2000
    }
}
