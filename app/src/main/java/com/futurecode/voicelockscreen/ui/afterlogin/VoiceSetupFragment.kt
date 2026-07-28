package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentVoiceSetupBinding

class VoiceSetupFragment : BaseFragment<FragmentVoiceSetupBinding>(FragmentVoiceSetupBinding::inflate) {

    private var timer: CountDownTimer? = null
    private var hasRecording = true


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnRecord.setOnClickListener { recordAgain() }
        binding.btnRecordAgain.setOnClickListener { recordAgain() }
        binding.btnContinue.setOnClickListener {
            saveVoicePhrase()
            findNavController().navigate(R.id.action_voiceSetupFragment_to_securityQuestionFragment)
        }
    }



    private fun recordAgain() {
        hasRecording = false
        binding.tvRecorded.text = getString(R.string.record_voice)
        binding.tvTimer.text = "0:00"
        binding.tvPhrase.text = ""

        timer?.cancel()
        timer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val second = 4 - (millisUntilFinished / 1000)
                binding.tvTimer.text = "0:0$second"
            }

            override fun onFinish() {
                hasRecording = true
                binding.tvTimer.text = "0:04"
                binding.tvRecorded.text = getString(R.string.recorded_successfully)
                binding.tvPhrase.text = getString(R.string.default_voice_phrase)
            }
        }.start()
    }

    private fun saveVoicePhrase() {
        if (!hasRecording) return
        requireContext()
            .getSharedPreferences(VOICE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VOICE_PHRASE, binding.tvPhrase.text.toString())
            .putBoolean(KEY_VOICE_RECORDED, true)
            .apply()
    }

    companion object {
        const val VOICE_PREFS = "voice_setup_prefs"
        const val KEY_VOICE_PHRASE = "voice_phrase"
        const val KEY_VOICE_RECORDED = "voice_recorded"
    }
}
