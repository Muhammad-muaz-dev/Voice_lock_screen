package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentVoiceGuideBinding

/** "Voice Setup" guide (reference copy): the two steps plus the permissions the lock needs. */
class VoiceGuideFragment : BaseFragment<FragmentVoiceGuideBinding>(FragmentVoiceGuideBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VoiceNoteUi.lightStatusBar(this)
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnStartVoiceSetup.setOnClickListener {
            findNavController().navigate(R.id.voiceSetupFragment, null, VoiceNoteUi.slideNavOptions)
        }
    }
}
