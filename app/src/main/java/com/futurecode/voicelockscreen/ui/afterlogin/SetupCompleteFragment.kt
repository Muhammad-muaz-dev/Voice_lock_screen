package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSetupCompleteBinding
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.service.LockService

class SetupCompleteFragment : BaseFragment<FragmentSetupCompleteBinding>(FragmentSetupCompleteBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LockService.enableAndStart(requireContext())
        binding.btnBack.setOnClickListener { goHome() }
        binding.btnHome.setOnClickListener { goHome() }
        binding.btnPin.setOnClickListener { findNavController().navigate(R.id.pinLockFragment) }
        binding.btnPattern.setOnClickListener { findNavController().navigate(R.id.patternLockFragment) }
        binding.btnFingerprint.setOnClickListener { findNavController().navigate(R.id.fingerprintLockFragment) }
        binding.btnPreview.setOnClickListener { findNavController().navigate(R.id.screenPreviewFragment) }
        binding.btnThemes.setOnClickListener { findNavController().navigate(R.id.seeAllthemesFragment) }
    }
    override fun onResume() {
        super.onResume()
        binding.btnPin.isVisible = !SecurePinStore.hasPin(requireContext())
        binding.btnPattern.isVisible = !SecurePatternStore.hasPattern(requireContext())
        binding.btnFingerprint.isVisible = !requireContext().getSharedPreferences(
            FingerprintLockFragment.FINGERPRINT_PREFS, 0
        ).getBoolean(FingerprintLockFragment.KEY_FINGERPRINT_ENABLED, false)
    }
    private fun goHome() { findNavController().popBackStack(R.id.homeFragment, false) }
}
