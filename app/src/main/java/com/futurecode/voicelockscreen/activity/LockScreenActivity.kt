package com.futurecode.voicelockscreen.activity

import android.app.NotificationManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.ResultReceiver
import android.util.Log
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import com.futurecode.voicelockscreen.activity.LockScreenViewModel.UnlockStatus
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ViewLockScreenBinding
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.ui.afterlogin.FingerprintLockFragment
import com.futurecode.voicelockscreen.utils.SpeechSession
import com.futurecode.voicelockscreen.utils.LockMethod
import com.futurecode.voicelockscreen.utils.LockScreenUi
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore
import com.futurecode.voicelockscreen.ui.custom.PatternLockView
import com.futurecode.voicelockscreen.utils.CredentialUnlockUi

class LockScreenActivity : BaseActivity() {
    private lateinit var binding: ViewLockScreenBinding
    private val viewModel: LockScreenViewModel by viewModels()
    private var selectedMethod = LockMethod.VOICE
    private val speechSession by lazy { SpeechSession(this) }
    private var fallbackOverlay: FrameLayout? = null
    private var isUnlocking = false
    private var didStartVoiceForCurrentResume = false


    override fun onCreate(savedInstanceState: Bundle?) {
        configureLockWindow()
        super.onCreate(savedInstanceState)
        getSystemService(NotificationManager::class.java)
            .cancel(LockService.LOCK_ALERT_NOTIFICATION_ID)
        binding = ViewLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ThemeWallpaperStore.applyTo(binding.imgWallpaper, this)
        LockScreenUi.bind(binding) { method ->
            selectedMethod = method
            LockScreenUi.select(binding, method)
            openSelectedUnlockMethod()
        }
        LockScreenUi.select(binding, selectedMethod)
        setupBackNavigation()

        viewModel.unlockStatus.observe(this) { status ->
            when (status) {
                is UnlockStatus.Success -> unlockAndClose(removeFallback = true)
                is UnlockStatus.Failure -> {
                    binding.tvSpeakHint.text = getString(status.messageRes)
                    if (status.messageRes == R.string.pattern_wrong) {
                        fallbackOverlay?.findViewById<TextView>(R.id.tvPatternStatus)
                            ?.setText(status.messageRes)
                        fallbackOverlay?.findViewById<PatternLockView>(R.id.patternView)?.clearPattern()
                    }
                    Toast.makeText(this, getString(status.messageRes), Toast.LENGTH_SHORT).show()
                }
                is UnlockStatus.Lockout -> {
                    binding.tvSpeakHint.text = getString(R.string.lockout_active) // Note: This resource needs to be added
                    fallbackOverlay?.findViewById<TextView>(R.id.tvPatternStatus)
                        ?.setText(R.string.lockout_active)
                    fallbackOverlay?.findViewById<PatternLockView>(R.id.patternView)?.clearPattern()
                    Toast.makeText(this, "Too many attempts. Locked out.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!didStartVoiceForCurrentResume && fallbackOverlay == null && !isUnlocking) {
            didStartVoiceForCurrentResume = true
            val fingerprint = intent.getBooleanExtra(EXTRA_FINGERPRINT, false)
            intent.removeExtra(EXTRA_FINGERPRINT)
            val startUnlock = Runnable {
                if (!didStartVoiceForCurrentResume || isFinishing || isDestroyed) return@Runnable
                if (fingerprint) startFingerprintUnlock() else startVoiceUnlock()
            }
            val handoff = IntentCompat.getParcelableExtra(intent, EXTRA_FIRST_FRAME_RECEIVER, ResultReceiver::class.java)
            intent.removeExtra(EXTRA_FIRST_FRAME_RECEIVER)
            if (handoff == null) {
                binding.root.post(startUnlock)
            } else {
                binding.root.doOnPreDraw { root ->
                    val finishHandoff = Runnable {
                        if (isFinishing || isDestroyed) return@Runnable
                        handoff.send(RESULT_OK, null)
                        root.post(startUnlock)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && root.isHardwareAccelerated) {
                        root.viewTreeObserver.registerFrameCommitCallback(finishHandoff)
                    } else {
                        root.post(finishHandoff)
                    }
                }
                binding.root.invalidate()
            }
        }
    }

    override fun onPause() {
        didStartVoiceForCurrentResume = false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        relockIfLeftWithoutUnlock()
    }

    private fun relockIfLeftWithoutUnlock() {
        if (isUnlocking || isChangingConfigurations) return
        if (AppLockState.isUnlockedThisSession(this)) return
        // Screen-off also stops this activity; LockService relocks on the next screen-on.
        if (getSystemService(PowerManager::class.java)?.isInteractive != true) return
        // Home/Recents must not bypass the lock, so hand it back to the service.
        LockService.showLockScreen(this)
    }

    override fun onDestroy() {
        removeFallbackOverlay()
        speechSession.release()
        super.onDestroy()
    }

    private fun configureLockWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
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
            isAppearanceLightStatusBars = false
            show(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun openSelectedUnlockMethod() {
        when (selectedMethod) {
            LockMethod.PIN -> showPinUnlockDialog()
            LockMethod.PATTERN -> showPatternUnlockDialog()
            LockMethod.VOICE -> startVoiceUnlock()
            LockMethod.FINGERPRINT -> startFingerprintUnlock()
        }
    }

    private fun startVoiceUnlock() {
        if (!SecureVoiceStore.hasPhrase(this)) {
            binding.tvSpeakHint.text = getString(R.string.save_voice_first)
            return
        }
        if (!SpeechSession.isAvailable(this)) {
            binding.tvSpeakHint.text = getString(R.string.speech_not_available)
            return
        }
        if (!SpeechSession.hasMicPermission(this)) {
            binding.tvSpeakHint.text = getString(R.string.microphone_blocked_body)
            return
        }

        selectedMethod = LockMethod.VOICE
        LockScreenUi.select(binding, selectedMethod)
        binding.tvSpeakHint.text = getString(R.string.listening_unlock)
        speechSession.start(SpeechSession.voiceIntent(this), voiceUnlockListener)
    }

    private val voiceUnlockListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Voice unlock: ready for speech")
            binding.tvSpeakHint.text = getString(R.string.listening_unlock)
        }

        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            Log.w(TAG, "Voice unlock: recognizer error $error")
            showVoiceFallback()
        }

        override fun onResults(results: Bundle?) {
            val spoken = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            viewModel.verifyVoice(this@LockScreenActivity, spoken)
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun showVoiceFallback() {
        binding.tvSpeakHint.text = getString(R.string.voice_try_again)
        Toast.makeText(this, R.string.voice_try_again, Toast.LENGTH_SHORT).show()
    }

    private fun showPinUnlockDialog() {
        if (!SecurePinStore.hasPin(this)) {
            Toast.makeText(this, R.string.pin_setup_required, Toast.LENGTH_SHORT).show()
            return
        }
        CredentialUnlockUi.bindPin(showFallbackOverlay(R.layout.dialog_pin_unlock), SecurePinStore.getPinLength(this)) { pin ->
            if (!isUnlocking) viewModel.verifyPin(applicationContext, pin)
        }
    }

    private fun showPatternUnlockDialog() {

        if (!SecurePatternStore.hasPattern(this)) {
            Toast.makeText(this, R.string.pattern_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = showFallbackOverlay(R.layout.dialog_pattern_unlock)
        val patternView = dialogView.findViewById<PatternLockView>(R.id.patternView).apply { useSetupAppearance(onWallpaper = true) }
        patternView.onPatternComplete = patternComplete@{ sequence ->
            if (isUnlocking) return@patternComplete
            viewModel.verifyPattern(applicationContext, sequence)
        }
    }

    private fun showFallbackOverlay(layoutRes: Int): View {
        removeFallbackOverlay()
        val overlay = FrameLayout(this).apply {
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
        addContentView(
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
        return ImageView(this).apply {
            setBackgroundResource(R.drawable.bg_circle_white)
            setImageResource(R.drawable.ic_back_blue)
            contentDescription = getString(R.string.back)
            setPadding(11.dp, 11.dp, 11.dp, 11.dp)
            elevation = 8.dp.toFloat()
            setOnClickListener { closeFallbackScreen() }
            layoutParams = FrameLayout.LayoutParams(42.dp, 42.dp, Gravity.TOP or Gravity.START).apply {
                marginStart = 20.dp
                topMargin = 20.dp
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fallbackOverlay != null) {
                    closeFallbackScreen()
                } else {
                    Toast.makeText(this@LockScreenActivity, R.string.voice_try_again, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun closeFallbackScreen() {
        removeFallbackOverlay()
        LockScreenUi.select(binding, selectedMethod)
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
        // Hand off to the system keyguard only after our own unlock succeeds.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        }
        binding.root.post {
            finishAndRemoveTask()
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()


    companion object {
        private const val TAG = "LockScreenActivity"
        private const val EXTRA_FINGERPRINT = "lock_fingerprint_requested"
        private const val EXTRA_FIRST_FRAME_RECEIVER = "lock_first_frame_receiver"
        private const val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

        fun newIntent(
            context: Context,
            fingerprint: Boolean = false,
            firstFrameReceiver: ResultReceiver? = null
        ): Intent {
            return Intent(context, LockScreenActivity::class.java).apply {
                putExtra(EXTRA_FINGERPRINT, fingerprint)
                firstFrameReceiver?.let { putExtra(EXTRA_FIRST_FRAME_RECEIVER, it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
        }
    }
}
