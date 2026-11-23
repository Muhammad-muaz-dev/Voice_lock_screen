package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.ViewLockScreenBinding
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.utils.SpeechSession
import com.futurecode.voicelockscreen.utils.LockMethod
import com.futurecode.voicelockscreen.utils.LockScreenUi
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.ui.custom.PatternLockView
import com.futurecode.voicelockscreen.utils.CredentialUnlockUi
import com.futurecode.voicelockscreen.utils.getTimeBasedGreetingRes
class ScreenPreviewFragment : BaseFragment<ViewLockScreenBinding>(ViewLockScreenBinding::inflate) {

    //tvGreeting
    private var selectedMethod = LockMethod.VOICE
    private var speechSession: SpeechSession? = null
    private var fallbackOverlay: FrameLayout? = null
    private var previousLightStatusBars = false
    private var previousStatusBarColor: Int = Color.TRANSPARENT
    private var previousNavigationBarColor: Int = Color.TRANSPARENT

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterPreviewMode()
        ThemeWallpaperStore.applyTo(binding.imgWallpaper, requireContext())
        LockScreenUi.bind(binding) { method ->
            selectedMethod = method
            LockScreenUi.select(binding, method)
            openSelectedUnlockMethod()
        }
        LockScreenUi.select(binding, selectedMethod)
        setupBackNavigation()

    }

    override fun onDestroyView() {
        removeFallbackOverlay()
        stopRecognizer()
        exitPreviewMode()
        super.onDestroyView()
    }

    private fun enterPreviewMode() {
        val window = requireActivity().window
        previousLightStatusBars = WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars
        previousStatusBarColor = window.statusBarColor
        previousNavigationBarColor = window.navigationBarColor
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            show(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun exitPreviewMode() {
        val window = requireActivity().window
        // The system navigation bar stays hidden app-wide (see SystemBars).
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = previousLightStatusBars
            show(WindowInsetsCompat.Type.statusBars())
        }
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = previousStatusBarColor
        window.navigationBarColor = previousNavigationBarColor
    }


    private fun openSelectedUnlockMethod() {
        when (selectedMethod) {
            LockMethod.PIN -> showPinUnlockDialog()
            LockMethod.PATTERN -> showPatternUnlockDialog()
            LockMethod.VOICE -> startVoiceUnlock()
            LockMethod.FINGERPRINT -> startFingerprintUnlock()
        }
    }

    private fun startFingerprintUnlock() {
        val enabled = requireContext()
            .getSharedPreferences(FingerprintLockFragment.FINGERPRINT_PREFS, Context.MODE_PRIVATE)
            .getBoolean(FingerprintLockFragment.KEY_FINGERPRINT_ENABLED, false)
        if (!enabled) {
            Toast.makeText(requireContext(), R.string.fingerprint_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        when (BiometricManager.from(requireContext()).canAuthenticate(BIOMETRIC_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> showFingerprintPrompt()
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Toast.makeText(requireContext(), R.string.fingerprint_not_supported, Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Toast.makeText(requireContext(), R.string.fingerprint_hardware_unavailable, Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Toast.makeText(requireContext(), R.string.fingerprint_none_enrolled, Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(requireContext(), R.string.fingerprint_not_supported, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFingerprintPrompt() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    binding.tvSpeakHint.text = getString(R.string.fingerprint_unlocked)
                    Toast.makeText(requireContext(), R.string.fingerprint_unlocked, Toast.LENGTH_SHORT).show()
                    unlockAndClose()
                }

                override fun onAuthenticationFailed() {
                    binding.tvSpeakHint.text = getString(R.string.fingerprint_failed)
                    Toast.makeText(requireContext(), R.string.fingerprint_failed, Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    binding.tvSpeakHint.text = errString
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.fingerprint_prompt_title))
                .setSubtitle(getString(R.string.fingerprint_prompt_subtitle))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .setAllowedAuthenticators(BIOMETRIC_AUTHENTICATORS)
                .build()
        )
    }

    private fun showPinUnlockDialog() {
        if (!SecurePinStore.hasPin(requireContext())) {
            Toast.makeText(requireContext(), R.string.pin_setup_required, Toast.LENGTH_SHORT).show()
            return
        }
        CredentialUnlockUi.bindPin(showFallbackOverlay(R.layout.dialog_pin_unlock), SecurePinStore.getPinLength(requireContext())) { pin ->
            if (SecurePinStore.verifyPin(requireContext(), pin)) {
                Toast.makeText(requireContext(), R.string.pin_unlocked, Toast.LENGTH_SHORT).show()
                removeFallbackOverlay()
                unlockAndClose()
            } else {
                Toast.makeText(requireContext(), R.string.pin_wrong, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPatternUnlockDialog() {
        if (!SecurePatternStore.hasPattern(requireContext())) {
            Toast.makeText(requireContext(), R.string.pattern_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = showFallbackOverlay(R.layout.dialog_pattern_unlock)
        val patternView = dialogView.findViewById<PatternLockView>(R.id.patternView).apply { useSetupAppearance(onWallpaper = true) }
        val status = dialogView.findViewById<TextView>(R.id.tvPatternStatus)

        patternView.onPatternComplete = { sequence ->
            if (SecurePatternStore.verifyPattern(requireContext(), sequence)) {
                binding.tvSpeakHint.text = getString(R.string.pattern_unlocked)
                Toast.makeText(requireContext(), R.string.pattern_unlocked, Toast.LENGTH_SHORT).show()
                removeFallbackOverlay()
                unlockAndClose()
            } else {
                status.text = getString(R.string.pattern_wrong)
                binding.tvSpeakHint.text = getString(R.string.pattern_wrong)
                Toast.makeText(requireContext(), R.string.pattern_wrong, Toast.LENGTH_SHORT).show()
                patternView.clearPattern()
            }
        }

    }

    private fun showFallbackOverlay(layoutRes: Int): View {
        removeFallbackOverlay()
        val overlay = FrameLayout(requireContext()).apply {
            CredentialUnlockUi.styleOverlay(this, binding.lockContent)
            isClickable = true
            isFocusable = true
        }
        val fallbackView = layoutInflater.inflate(layoutRes, overlay, false)
        overlay.addView(
            fallbackView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        overlay.addView(createFallbackBackButton())
        binding.root.addView(
            overlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        CredentialUnlockUi.requestInsets(overlay)
        fallbackOverlay = overlay
        return fallbackView
    }

    private fun createFallbackBackButton(): ImageView {
        return ImageView(requireContext()).apply {
            setBackgroundResource(R.drawable.bg_circle_white)
            setImageResource(R.drawable.ic_back_blue)
            contentDescription = getString(R.string.back)
            setPadding(11.dp, 11.dp, 11.dp, 11.dp)
            elevation = 8.dp.toFloat()
            setOnClickListener { removeFallbackOverlay() }
            layoutParams = FrameLayout.LayoutParams(42.dp, 42.dp, Gravity.TOP or Gravity.START).apply {
                marginStart = 20.dp
                topMargin = 20.dp
            }
        }
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fallbackOverlay != null) {
                    removeFallbackOverlay()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun removeFallbackOverlay() {
        fallbackOverlay?.let { overlay ->
            (overlay.parent as? ViewGroup)?.removeView(overlay)
        }
        fallbackOverlay = null
    }

    private fun startVoiceUnlock() {
        if (!SecureVoiceStore.hasPhrase(requireContext())) {
            Toast.makeText(requireContext(), R.string.save_voice_first, Toast.LENGTH_SHORT).show()
            return
        }

        if (!SpeechSession.isAvailable(requireContext())) {
            Toast.makeText(requireContext(), R.string.speech_not_available, Toast.LENGTH_SHORT).show()
            return
        }
        if (!SpeechSession.hasMicPermission(requireContext())) {
            binding.tvSpeakHint.text = getString(R.string.microphone_blocked_body)
            return
        }

        binding.tvSpeakHint.text = getString(R.string.listening_unlock)

        val session = speechSession ?: SpeechSession(requireContext()).also { speechSession = it }
        session.start(
            SpeechSession.voiceIntent(requireContext()),
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    binding.tvSpeakHint.text = getString(R.string.listening_unlock)
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    showTryAgain()
                }

                override fun onResults(results: Bundle?) {
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (SecureVoiceStore.verifyPhrase(requireContext(), spoken)) {
                        binding.tvSpeakHint.text = getString(R.string.voice_unlocked)
                        Toast.makeText(requireContext(), R.string.voice_unlocked, Toast.LENGTH_SHORT).show()
                        unlockAndClose()
                    } else {
                        showTryAgain()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }
        )
    }

    private fun showTryAgain() {
        binding.tvSpeakHint.text = getString(R.string.voice_try_again)
        Toast.makeText(requireContext(), R.string.voice_try_again, Toast.LENGTH_SHORT).show()
    }

    private fun unlockAndClose() {
        AppLockState.setUnlockedThisSession(requireContext(), true)

        binding.root.post {
            val navController = findNavController()
            if (!navController.popBackStack()) {
                navController.navigate(R.id.homeFragment)
            }
        }
    }

    private fun stopRecognizer() {
        speechSession?.release()
        speechSession = null
    }


    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK
        const val LOCK_SESSION_PREFS = "lock_session_prefs"
        const val KEY_UNLOCKED_THIS_SESSION = "unlocked_this_session"
    }
}
