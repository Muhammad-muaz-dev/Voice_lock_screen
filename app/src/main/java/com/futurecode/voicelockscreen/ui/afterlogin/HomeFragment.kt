package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentHomeBinding
import com.futurecode.voicelockscreen.utils.LockPermissionHelper

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivSettings.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingFragment)
        }

        binding.voiceUnlock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_VOICE,
                R.id.action_homeFragment_to_voiceSetupFragment
            )
        }

        binding.pinLock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_PIN,
                R.id.action_homeFragment_to_pinLockFragment
            )
        }

        binding.patternLock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_PATTERN,
                R.id.action_homeFragment_to_patternLockFragment
            )
        }

        binding.fingerLock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_FINGERPRINT,
                R.id.action_homeFragment_to_fingerprintLockFragment
            )
        }

        binding.preview.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_screenPreviewFragment)
        }

        binding.tvSeeAllTools.setOnClickListener {

        }

        binding.tvSeeAllThemes.setOnClickListener {
         findNavController().navigate(R.id.action_homeFragment_to_seeAllthemesFragment)
        }

        binding.ivPremium.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)

        }

        binding.ivMic.setOnClickListener {

        }
    }

    private fun openFeatureWithPermission(target: String, @IdRes directActionId: Int) {
        if (hasRequiredPermissions()) {
            findNavController().navigate(directActionId)
            return
        }

        findNavController().navigate(
            R.id.action_homeFragment_to_permissionFragment,
            Bundle().apply {
                putString(PermissionFragment.ARG_TARGET, target)
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return hasPermission(Manifest.permission.RECORD_AUDIO) &&
            hasPermission(Manifest.permission.READ_PHONE_STATE) &&
            LockPermissionHelper.hasOverlayPermission(requireContext()) &&
            LockPermissionHelper.isIgnoringBatteryOptimizations(requireContext())
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

}
