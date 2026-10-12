package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentCameraVoiceBinding
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore

class CameraVoiceFragment : BaseFragment<FragmentCameraVoiceBinding>(FragmentCameraVoiceBinding::inflate) {

    private var step = 0
    private var recordings = 0
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) goToRecord()
        else Toast.makeText(requireContext(), R.string.camera_access_desc, Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        step = savedInstanceState?.getInt("step") ?: if (VoiceLockFeatureStore.isCameraEnabled(requireContext())) 4 else 0
        recordings = savedInstanceState?.getInt("recordings") ?: 0
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.switchCamera.isChecked = VoiceLockFeatureStore.isCameraEnabled(requireContext())
        binding.switchCamera.setOnCheckedChangeListener { _, checked ->
            VoiceLockFeatureStore.setCameraEnabled(requireContext(), checked)
        }
        binding.btnPrimary.setOnClickListener { onPrimary() }
        render()
    }

    override fun onDestroyView() {
        listening = false
        recognizer?.cancel()
        recognizer?.destroy()
        super.onDestroyView()
    }

    private fun onPrimary() {
        when (step) {
            0 -> {
                step = 1
                render()
            }
            1 -> {
                if (listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).all {
                    ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
                }
                ) goToRecord()
                else cameraPermission.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }
            2 -> startListen()
            3 -> {
                VoiceLockFeatureStore.setCameraEnabled(requireContext(), true)
                step = 4
                render()
            }
            else -> findNavController().popBackStack()
        }
    }

    private fun goToRecord() {
        step = 2
        render()
    }

    private fun startListen() {
        if (listening) return
        if (ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            cameraPermission.launch(arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO)); return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), R.string.speech_not_available, Toast.LENGTH_SHORT).show()
            return
        }
        recognizer?.destroy()
        listening = true
        binding.btnPrimary.isEnabled = false
        recognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    binding.tvHeard.isVisible = true
                    binding.tvHeard.setText(R.string.recording_listening)
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) {
                    listening = false
                    if (bindingOrNull == null) return
                    binding.btnPrimary.isEnabled = true
                    binding.tvHeard.setText(R.string.we_couldnt_hear)
                }
                override fun onResults(results: Bundle?) {
                    listening = false
                    if (bindingOrNull == null) return
                    binding.btnPrimary.isEnabled = true
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (VoiceSetupFragment.normalizePhrase(spoken) != "open camera") {
                        binding.tvHeard.setText(R.string.we_couldnt_hear)
                        return
                    }
                    recordings += 1
                    VoiceLockFeatureStore.saveCameraPhrase(requireContext(), spoken)
                    binding.tvHeard.text = getString(R.string.confirm_your_voice) + "\n$spoken ($recordings/3)"
                    if (recordings >= 3) {
                        step = 3
                        render()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        recognizer?.startListening(intent)
    }

    private fun render() {
        binding.tvHeard.isVisible = step == 2
        binding.toggleRow.isVisible = step == 4
        binding.btnPrimary.isEnabled = true
        when (step) {
            0 -> {
                binding.tvHeadline.setText(R.string.camera_how_it_works)
                binding.tvBody.setText(R.string.camera_two_steps)
                binding.cardStep.isVisible = true
                binding.toggleRow.isVisible = false
                binding.btnPrimary.setText(R.string.next)
            }
            1 -> {
                binding.tvHeadline.setText(R.string.camera_access)
                binding.tvBody.setText(R.string.camera_access_desc)
                binding.cardStep.isVisible = false
                binding.btnPrimary.setText(R.string.allow)
            }
            2 -> {
                binding.tvHeadline.setText(R.string.say_open_camera)
                binding.tvBody.setText(R.string.tap_to_record)
                binding.tvHeard.isVisible = true
                binding.btnPrimary.setText(R.string.tap_to_record)
            }
            3 -> {
                binding.tvHeadline.setText(R.string.youre_all_set)
                binding.tvBody.setText(R.string.say_open_camera)
                binding.btnPrimary.setText(R.string.lets_go)
            }
            else -> {
                binding.switchCamera.isChecked = VoiceLockFeatureStore.isCameraEnabled(requireContext())
                binding.tvHeadline.setText(R.string.open_camera_by_voice)
                binding.tvBody.setText(R.string.camera_voice_desc)
                binding.toggleRow.isVisible = true
                binding.cardStep.isVisible = false
                binding.btnPrimary.setText(R.string.re_record_voice)
                binding.btnPrimary.setOnClickListener {
                    recordings = 0
                    step = 2
                    render()
                    binding.btnPrimary.setOnClickListener { onPrimary() }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("step", step)
        outState.putInt("recordings", recordings)
    }

    companion object {
        fun openCamera(context: android.content.Context) {
            runCatching {
                context.startActivity(
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
