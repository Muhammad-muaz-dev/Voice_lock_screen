package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentFingerprintLockBinding
import com.futurecode.voicelockscreen.service.LockMonitorService

class FingerprintLockFragment : BaseFragment<FragmentFingerprintLockBinding>(FragmentFingerprintLockBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEnable.setOnClickListener {
            checkSupportAndAuthenticate()
        }

        if (isFingerprintEnabled()) {
            binding.tvFingerprintStatus.text = getString(R.string.fingerprint_enabled)
        }
    }

    private fun checkSupportAndAuthenticate() {
        when (BiometricManager.from(requireContext()).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> startBiometricPrompt()
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> showStatus(R.string.fingerprint_not_supported)
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> showStatus(R.string.fingerprint_hardware_unavailable)
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> showStatus(R.string.fingerprint_none_enrolled)
            else -> showStatus(R.string.fingerprint_not_supported)
        }
    }

    private fun startBiometricPrompt() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    saveFingerprintEnabled()
                    LockMonitorService.startIfNeeded(requireContext())
                    showStatus(R.string.fingerprint_enabled)
                    Toast.makeText(requireContext(), R.string.fingerprint_enabled, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }

                override fun onAuthenticationFailed() {
                    showStatus(R.string.fingerprint_failed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    binding.tvFingerprintStatus.text = errString
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.fingerprint_prompt_title))
                .setSubtitle(getString(R.string.fingerprint_prompt_subtitle))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
        )
    }

    private fun showStatus(messageRes: Int) {
        binding.tvFingerprintStatus.text = getString(messageRes)
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun saveFingerprintEnabled() {
        requireContext()
            .getSharedPreferences(FINGERPRINT_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FINGERPRINT_ENABLED, true)
            .apply()
    }

    private fun isFingerprintEnabled(): Boolean {
        return requireContext()
            .getSharedPreferences(FINGERPRINT_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_FINGERPRINT_ENABLED, false)
    }

    companion object {
        const val FINGERPRINT_PREFS = "fingerprint_lock_prefs"
        const val KEY_FINGERPRINT_ENABLED = "fingerprint_enabled"
        private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
}
