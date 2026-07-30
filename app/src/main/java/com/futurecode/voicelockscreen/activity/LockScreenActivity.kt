package com.futurecode.voicelockscreen.activity

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.FragmentScreenPreviewBinding
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.ui.afterlogin.FingerprintLockFragment
import com.futurecode.voicelockscreen.ui.custom.PatternLockView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LockScreenActivity : BaseActivity() {
    private lateinit var binding: FragmentScreenPreviewBinding
    private var selectedMethod = VOICE_INDEX
    private var speechRecognizer: SpeechRecognizer? = null
    private var fallbackOverlay: FrameLayout? = null
    private var isUnlocking = false

    private val methods = listOf(
        PreviewMethod("PIN", R.drawable.ic_pin_lock),
        PreviewMethod("Pattern", R.drawable.ic_pattern_lock),
        PreviewMethod("Voice", R.drawable.ic_mic_blue),
        PreviewMethod("Fingerprint", R.drawable.ic_fingerprint_lock)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        configureLockWindow()
        super.onCreate(savedInstanceState)
        binding = FragmentScreenPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindDateTime()
        bindMethods()
        updateSelectedMethod()
        binding.micRings.setOnClickListener { openSelectedUnlockMethod() }
        binding.root.post { startVoiceUnlock() }
    }

    override fun onDestroy() {
        removeFallbackOverlay()
        stopRecognizer()
        super.onDestroy()
    }

    override fun onBackPressed() {
        Toast.makeText(this, R.string.voice_try_again, Toast.LENGTH_SHORT).show()
    }

    private fun configureLockWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
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
            container.findViewById<ImageView>(R.id.imgMethod).setImageResource(method.iconRes)
            container.findViewById<TextView>(R.id.tvMethod).text = method.title
            container.setOnClickListener {
                selectedMethod = index
                updateSelectedMethod()
                openSelectedUnlockMethod()
            }
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
                ContextCompat.getColor(this, if (selected) R.color.blue_primary else R.color.white)
            )
            label.setTextColor(
                ContextCompat.getColor(this, if (selected) R.color.blue_primary else R.color.white)
            )
        }

        binding.tvSpeakHint.text = when (selectedMethod) {
            PIN_INDEX -> getString(R.string.pin_unlock)
            PATTERN_INDEX -> "Draw your pattern to unlock"
            VOICE_INDEX -> getString(R.string.tap_microphone)
            else -> "Touch fingerprint sensor"
        }
    }

    private fun openSelectedUnlockMethod() {
        when (selectedMethod) {
            PIN_INDEX -> showPinUnlockDialog()
            PATTERN_INDEX -> showPatternUnlockDialog()
            VOICE_INDEX -> startVoiceUnlock()
            FINGERPRINT_INDEX -> startFingerprintUnlock()
        }
    }

    private fun startVoiceUnlock() {
        if (!SecureVoiceStore.hasPhrase(this)) {
            binding.tvSpeakHint.text = getString(R.string.save_voice_first)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            binding.tvSpeakHint.text = getString(R.string.speech_not_available)
            return
        }

        stopRecognizer()
        selectedMethod = VOICE_INDEX
        updateSelectedMethod()
        binding.tvSpeakHint.text = getString(R.string.listening_unlock)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    binding.tvSpeakHint.text = getString(R.string.listening_unlock)
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    showVoiceFallback()
                }

                override fun onResults(results: Bundle?) {
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (SecureVoiceStore.verifyPhrase(this@LockScreenActivity, spoken)) {
                        unlockAndClose()
                    } else {
                        showVoiceFallback()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            startListening(speechIntent())
        }
    }

    private fun showVoiceFallback() {
        binding.tvSpeakHint.text = getString(R.string.voice_try_again)
        Toast.makeText(this, R.string.voice_try_again, Toast.LENGTH_SHORT).show()
    }

    private fun speechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun stopRecognizer() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun showPinUnlockDialog() {
        if (!SecurePinStore.hasPin(this)) {
            Toast.makeText(this, R.string.pin_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        val pinLength = SecurePinStore.getPinLength(this)
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
            if (isUnlocking) return@setOnClickListener
            if (enteredPin.length != pinLength) {
                Toast.makeText(this, R.string.pin_incomplete, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (SecurePinStore.verifyPin(this, enteredPin.toString())) {
                unlockAndClose(removeFallback = true)
            } else {
                enteredPin.clear()
                setupPinDialogDots(dots, pinLength, enteredPin.length)
                binding.tvSpeakHint.text = getString(R.string.pin_wrong)
                Toast.makeText(this, R.string.pin_wrong, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupPinDialogDots(container: LinearLayout, pinLength: Int, enteredLength: Int) {
        container.removeAllViews()
        repeat(pinLength) { index ->
            val dot = View(this)
            dot.background = ContextCompat.getDrawable(
                this,
                if (index < enteredLength) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot_empty
            )
            container.addView(dot, LinearLayout.LayoutParams(16.dp, 16.dp).apply {
                marginStart = 8.dp
                marginEnd = 8.dp
            })
        }
    }

    private fun showPatternUnlockDialog() {
        if (!SecurePatternStore.hasPattern(this)) {
            Toast.makeText(this, R.string.pattern_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = showFallbackOverlay(R.layout.dialog_pattern_unlock)
        val patternView = dialogView.findViewById<PatternLockView>(R.id.patternView)
        val status = dialogView.findViewById<TextView>(R.id.tvPatternStatus)

        patternView.onPatternComplete = patternComplete@{ sequence ->
            if (isUnlocking) return@patternComplete
            if (SecurePatternStore.verifyPattern(this, sequence)) {
                patternView.onPatternComplete = null
                unlockAndClose(removeFallback = true)
            } else {
                status.text = getString(R.string.pattern_wrong)
                binding.tvSpeakHint.text = getString(R.string.pattern_wrong)
                Toast.makeText(this, R.string.pattern_wrong, Toast.LENGTH_SHORT).show()
                patternView.clearPattern()
            }
        }
    }

    private fun showFallbackOverlay(layoutRes: Int): View {
        removeFallbackOverlay()
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0x99000000.toInt())
            isClickable = true
            isFocusable = true
        }
        val fallbackView = layoutInflater.inflate(layoutRes, overlay, false)
        overlay.addView(
            fallbackView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                leftMargin = 28.dp
                rightMargin = 28.dp
            }
        )
        addContentView(
            overlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        fallbackOverlay = overlay
        return fallbackView
    }

    private fun removeFallbackOverlay() {
        val overlay = fallbackOverlay
        fallbackOverlay = null
        overlay?.post {
            (overlay.parent as? ViewGroup)?.removeView(overlay)
        }
    }

    private fun startFingerprintUnlock() {
        val enabled = getSharedPreferences(FingerprintLockFragment.FINGERPRINT_PREFS, Context.MODE_PRIVATE)
            .getBoolean(FingerprintLockFragment.KEY_FINGERPRINT_ENABLED, false)
        if (!enabled) {
            Toast.makeText(this, R.string.fingerprint_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        when (BiometricManager.from(this).canAuthenticate(BIOMETRIC_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> showFingerprintPrompt()
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Toast.makeText(this, R.string.fingerprint_not_supported, Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Toast.makeText(this, R.string.fingerprint_hardware_unavailable, Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Toast.makeText(this, R.string.fingerprint_none_enrolled, Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this, R.string.fingerprint_not_supported, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFingerprintPrompt() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlockAndClose()
                }

                override fun onAuthenticationFailed() {
                    binding.tvSpeakHint.text = getString(R.string.fingerprint_failed)
                    Toast.makeText(this@LockScreenActivity, R.string.fingerprint_failed, Toast.LENGTH_SHORT).show()
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

    private fun unlockAndClose(removeFallback: Boolean = false) {
        if (isUnlocking) return
        isUnlocking = true
        AppLockState.setUnlockedThisSession(this, true)
        if (removeFallback) removeFallbackOverlay()
        Toast.makeText(this, R.string.voice_unlocked, Toast.LENGTH_SHORT).show()
        binding.root.post {
            finishAndRemoveTask()
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private data class PreviewMethod(
        val title: String,
        val iconRes: Int
    )

    companion object {
        private const val PIN_INDEX = 0
        private const val PATTERN_INDEX = 1
        private const val VOICE_INDEX = 2
        private const val FINGERPRINT_INDEX = 3
        private const val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

        fun newIntent(context: Context): Intent {
            return Intent(context, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
        }
    }
}
