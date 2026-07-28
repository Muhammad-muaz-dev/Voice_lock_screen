package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentHomeBinding

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivSettings.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingFragment)
        }

        binding.voiceUnlock.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_voiceSetupFragment)
        }

        binding.pinLock.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_pinLockFragment)
        }

        binding.patternLock.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_patternLockFragment)
        }

        binding.fingerLock.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_fingerprintLockFragment)
        }

        binding.preview.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_screenPreviewFragment)
        }

        binding.tvSeeAllTools.setOnClickListener {

        }

        binding.tvSeeAllThemes.setOnClickListener {

        }

        binding.ivPremium.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)

        }

        binding.ivMic.setOnClickListener {

        }
    }
}