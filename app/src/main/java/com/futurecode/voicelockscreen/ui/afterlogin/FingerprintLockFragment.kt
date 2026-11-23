package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentFingerprintLockBinding
import com.futurecode.voicelockscreen.service.LockService

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
            showStatus(R.string.fingerprint_enabled)
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
                    LockService.enableAndStart(requireContext())
                    showSetupSuccess(R.string.fingerprint_enabled) { findNavController().popBackStack() }
                }

                override fun onAuthenticationFailed() {
                    showStatus(R.string.fingerprint_failed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    renderStatus(errString, isError = true)
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
        renderStatus(getString(messageRes), isError = messageRes != R.string.fingerprint_enabled)
    }

    private fun renderStatus(message: CharSequence, isError: Boolean) {
        val status = bindingOrNull?.tvFingerprintStatus ?: return
        status.text = message
        status.setTextColor(ContextCompat.getColor(status.context, if (isError) R.color.error else R.color.successStrong))
        status.isVisible = true
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
