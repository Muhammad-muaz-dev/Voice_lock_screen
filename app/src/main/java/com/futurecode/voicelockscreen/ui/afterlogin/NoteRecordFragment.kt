package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentNoteRecordBinding
import com.futurecode.voicelockscreen.utils.SpeechSession
import com.futurecode.voicelockscreen.utils.VoiceLanguagePicker
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.utils.VoiceNoteItem
import java.util.Locale

class NoteRecordFragment : BaseFragment<FragmentNoteRecordBinding>(FragmentNoteRecordBinding::inflate) {
    private var speechSession: SpeechSession? = null
    private var listening = false
    private var startedAt = 0L
    private var saving = false
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (!listening || bindingOrNull == null) return
            val elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1000
            binding.tvTimer.text = String.format(Locale.ROOT, "%02d:%02d", elapsed / 60, elapsed % 60)
            handler.postDelayed(this, 250)
        }
    }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else binding.tvTranscript.setText(R.string.microphone_blocked_body)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnLanguage.text = VoiceLanguagePicker.label(requireContext())
        binding.btnLanguage.setOnClickListener { VoiceLanguagePicker.show(requireContext()) { binding.btnLanguage.text = VoiceLanguagePicker.label(requireContext()) } }
        binding.btnStart.setOnClickListener {
            when {
                listening -> { binding.btnStart.isEnabled = false; speechSession?.stopListening() }
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED -> permission.launch(Manifest.permission.RECORD_AUDIO)
                else -> start()
            }
        }
    }
    private fun start() {
        if (!prefManager.isPro && VoiceLockFeatureStore.remainingTries(requireContext()) <= 0) {
            findNavController().navigate(R.id.premiumFragment); return
        }
        if (!SpeechSession.isAvailable(requireContext())) { binding.tvTranscript.setText(R.string.speech_not_available); return }
        listening = true; saving = false; startedAt = SystemClock.elapsedRealtime()
        binding.btnStart.setText(R.string.stop)
        binding.btnStart.setBackgroundColor(requireContext().getColor(R.color.danger))
        binding.btnLanguage.isEnabled = false
        handler.post(tick)
        val session = speechSession ?: SpeechSession(requireContext()).also { speechSession = it }
        session.start(
            SpeechSession.voiceIntent(requireContext(), partialResults = true),
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { bindingOrNull?.tvTranscript?.setText(R.string.recording_listening) }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) {
                    if (bindingOrNull == null) return
                    reset(); binding.tvTranscript.setText(R.string.cant_hear_you)
                }
                override fun onResults(results: Bundle?) {
                    if (bindingOrNull == null || saving) return
                    val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
                    val duration = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt().coerceAtLeast(1)
                    reset()
                    if (transcript.isBlank()) { binding.tvTranscript.setText(R.string.cant_hear_you); return }
                    saving = true
                    val now = System.currentTimeMillis()
                    VoiceLockFeatureStore.saveNote(requireContext(), VoiceNoteItem(now, VoiceLockFeatureStore.titleFrom(transcript),transcript,VoiceLockFeatureStore.summarize(transcript),now,duration))
                    if (!prefManager.isPro) VoiceLockFeatureStore.consumeTry(requireContext())
                    findNavController().navigate(R.id.noteDetailFragment, Bundle().apply { putLong("note_id",now) },navOptions { popUpTo(R.id.noteRecordFragment) { inclusive = true } })
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!partial.isNullOrBlank()) bindingOrNull?.tvTranscript?.text = partial
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }
        )
    }
    private fun reset() {
        listening = false; handler.removeCallbacks(tick)
        binding.btnStart.isEnabled = true; binding.btnStart.setText(R.string.start)
        binding.btnStart.setBackgroundResource(R.drawable.bg_cta_gradient)
        binding.btnLanguage.isEnabled = true
    }
    override fun onDestroyView() {
        listening = false; handler.removeCallbacksAndMessages(null)
        speechSession?.release(); speechSession = null
        super.onDestroyView()
    }
}
