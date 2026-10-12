package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentScreenPreviewBinding
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.utils.VoiceLockPattern
import com.futurecode.voicelockscreen.utils.getTimeBasedGreetingRes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class ScreenPreviewFragment : BaseFragment<FragmentScreenPreviewBinding>(FragmentScreenPreviewBinding::inflate) {

    //tvGreeting
    private val methods = listOf(
        PreviewMethod(R.string.pin, R.drawable.ic_pin_lock),
        PreviewMethod(R.string.pattern, R.drawable.ic_pattern_lock),
        PreviewMethod(R.string.voice, R.drawable.ic_mic_blue),
        PreviewMethod(R.string.fingerprint, R.drawable.ic_fingerprint_lock)
    )
    private var selectedMethod = 2
    private var speechRecognizer: SpeechRecognizer? = null
    private var fallbackOverlay: FrameLayout? = null
    private var previousStatusBarColor: Int = Color.TRANSPARENT
    private var previousNavigationBarColor: Int = Color.TRANSPARENT

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterPreviewMode()
        ThemeWallpaperStore.applyTo(binding.imgWallpaper, requireContext())
        bindDateTime()
        bindMethods()
        updateSelectedMethod()
        setupBackNavigation()

        binding.micRings.setOnClickListener {
            openSelectedUnlockMethod()
        }

        ThemeWallpaperStore.applyContent(binding, requireContext())
        binding.tvSpeakHint.setText(R.string.listening_unlock_hint)
    }

    override fun onDestroyView() {
        removeFallbackOverlay()
        stopRecognizer()
        exitPreviewMode()
        super.onDestroyView()
    }

    private fun enterPreviewMode() {
        val window = requireActivity().window
        previousStatusBarColor = window.statusBarColor
        previousNavigationBarColor = window.navigationBarColor
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun exitPreviewMode() {
        val window = requireActivity().window
        WindowInsetsControllerCompat(window, window.decorView).show(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
        )
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = previousStatusBarColor
        window.navigationBarColor = previousNavigationBarColor
    }


    private fun bindDateTime() {
        val now = Date()
        binding.tvStatusTime.text = SimpleDateFormat("H:mm", Locale.getDefault()).format(now)
        binding.tvMainTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        binding.tvDate.text = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now)
    }

    private fun bindMethods() {
        binding.methodsRow.children.forEachIndexed { index, child ->
            val method = methods[index]
            val container = child as LinearLayout
            val icon = container.findViewById<ImageView>(R.id.imgMethod)
            val label = container.findViewById<TextView>(R.id.tvMethod)
            icon.setImageResource(method.iconRes)
            label.setText(method.titleRes)
            container.setOnClickListener {
                selectedMethod = index
                updateSelectedMethod()
                openSelectedUnlockMethod()
            }
        }
    }

    private fun openSelectedUnlockMethod() {
        when (selectedMethod) {
            0 -> showPinUnlockDialog()
            1 -> showPatternUnlockDialog()
            2 -> startVoiceUnlock()
            3 -> startFingerprintUnlock()
        }
    }

    private fun updateSelectedMethod() {
        binding.methodsRow.children.forEachIndexed { index, child ->
            val container = child as LinearLayout
            val icon = container.findViewById<ImageView>(R.id.imgMethod)
            val label = container.findViewById<TextView>(R.id.tvMethod)
            val selected = index == selectedMethod
            container.setBackgroundResource(
                if (selected) R.drawable.bg_preview_method_selected else R.drawable.bg_preview_method
            )
            icon.setBackgroundResource(
                if (selected) R.drawable.bg_preview_method_icon_selected else R.drawable.bg_preview_method_icon
            )
            icon.setColorFilter(
                ContextCompat.getColor(requireContext(), if (selected) R.color.blue_primary else R.color.white)
            )
            label.setTextColor(
                ContextCompat.getColor(requireContext(), if (selected) R.color.blue_primary else R.color.white)
            )
        }

        binding.tvSpeakHint.text = when (selectedMethod) {
            0 -> getString(R.string.pin_unlock)
            1 -> getString(R.string.draw_pattern_to_unlock)
            2 -> getString(R.string.tap_microphone)
            else -> getString(R.string.touch_fingerprint_sensor)
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

        val pinLength = SecurePinStore.getPinLength(requireContext())
        val enteredPin = StringBuilder()
        val dialogView = showFallbackOverlay(R.layout.dialog_pin_unlock)
        val dots = dialogView.findViewById<LinearLayout>(R.id.pinDotsContainer)
        val buttons = listOf(
            R.id.btnOne to "1",
            R.id.btnTwo to "2",
            R.id.btnThree to "3",
            R.id.btnFour to "4",
            R.id.btnFive to "5",
            R.id.btnSix to "6",
            R.id.btnSeven to "7",
            R.id.btnEight to "8",
            R.id.btnNine to "9",
            R.id.btnZero to "0"
        )

        setupPinDialogDots(dots, pinLength, enteredPin.length)
        buttons.forEach { (id, digit) ->
            dialogView.findViewById<View>(id).setOnClickListener {
                if (enteredPin.length < pinLength) {
                    enteredPin.append(digit)
                    setupPinDialogDots(dots, pinLength, enteredPin.length)
                }
            }
        }

        dialogView.findViewById<View>(R.id.btnBackspace).setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.lastIndex)
                setupPinDialogDots(dots, pinLength, enteredPin.length)
            }
        }

        dialogView.findViewById<View>(R.id.btnConfirm).setOnClickListener {
            if (enteredPin.length != pinLength) {
                Toast.makeText(requireContext(), R.string.pin_incomplete, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (SecurePinStore.verifyPin(requireContext(), enteredPin.toString())) {
                binding.tvSpeakHint.text = getString(R.string.pin_unlocked)
                Toast.makeText(requireContext(), R.string.pin_unlocked, Toast.LENGTH_SHORT).show()
                removeFallbackOverlay()
                unlockAndClose()
            } else {
                enteredPin.clear()
                setupPinDialogDots(dots, pinLength, enteredPin.length)
                binding.tvSpeakHint.text = getString(R.string.pin_wrong)
                Toast.makeText(requireContext(), R.string.pin_wrong, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setupPinDialogDots(container: LinearLayout, pinLength: Int, enteredLength: Int) {
        container.removeAllViews()
        repeat(pinLength) { index ->
            val dot = View(requireContext())
            dot.background = ContextCompat.getDrawable(
                requireContext(),
                if (index < enteredLength) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot_empty
            )
            container.addView(dot, LinearLayout.LayoutParams(16.dp, 16.dp).apply {
                marginStart = 8.dp
                marginEnd = 8.dp
            })
        }
    }

    private fun showPatternUnlockDialog() {
        if (!SecurePatternStore.hasPattern(requireContext())) {
            Toast.makeText(requireContext(), R.string.pattern_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = showFallbackOverlay(R.layout.dialog_pattern_unlock)
        val patternView = dialogView.findViewById<VoiceLockPattern>(R.id.patternView)
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
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_color))
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

        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), R.string.speech_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        stopRecognizer()
        binding.tvSpeakHint.text = getString(R.string.listening_unlock)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
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
            })
            startListening(speechIntent())
        }
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
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun speechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private data class PreviewMethod(
        val titleRes: Int,
        val iconRes: Int
    )

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK
        const val LOCK_SESSION_PREFS = "lock_session_prefs"
        const val KEY_UNLOCKED_THIS_SESSION = "unlocked_this_session"
    }
}

