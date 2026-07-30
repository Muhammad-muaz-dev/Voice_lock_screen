package com.futurecode.voicelockscreen.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.databinding.FragmentScreenPreviewBinding
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.ui.afterlogin.FingerprintLockFragment
import com.futurecode.voicelockscreen.ui.afterlogin.VoiceSetupFragment
import com.futurecode.voicelockscreen.ui.custom.PatternLockView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LockOverlayController(private val context: Context) {
    private val themedContext = ContextThemeWrapper(context, R.style.Theme_VoiceLockScreen)
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val inflater = LayoutInflater.from(themedContext)
    private val methods = listOf(
        PreviewMethod("PIN", R.drawable.ic_pin_lock),
        PreviewMethod("Pattern", R.drawable.ic_pattern_lock),
        PreviewMethod("Voice", R.drawable.ic_mic_blue),
        PreviewMethod("Fingerprint", R.drawable.ic_fingerprint_lock)
    )

    private var container: FrameLayout? = null
    private var binding: FragmentScreenPreviewBinding? = null
    private var selectedMethod = 2
    private var speechRecognizer: SpeechRecognizer? = null

    fun show() {
        if (!canDrawOverlays()) {
            openActivityPreview()
            return
        }
        if (container != null) return

        val root = FrameLayout(themedContext)
        val previewBinding = FragmentScreenPreviewBinding.inflate(inflater, root, false)
        root.addView(
            previewBinding.root,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        container = root
        binding = previewBinding
        windowManager.addView(root, overlayLayoutParams())

        bindDateTime(previewBinding)
        bindMethods(previewBinding)
        updateSelectedMethod(previewBinding)
        previewBinding.micRings.setOnClickListener {
            openSelectedUnlockMethod()
        }
    }

    fun dismiss(unlocked: Boolean = false) {
        if (unlocked) AppLockState.setUnlockedThisSession(context, true)
        stopRecognizer()
        container?.let { runCatching { windowManager.removeView(it) } }
        container = null
        binding = null
    }

    private fun bindDateTime(binding: FragmentScreenPreviewBinding) {
        val now = Date()
        binding.tvStatusTime.text = SimpleDateFormat("H:mm", Locale.getDefault()).format(now)
        binding.tvMainTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        binding.tvDate.text = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now)
    }

    private fun bindMethods(binding: FragmentScreenPreviewBinding) {
        binding.methodsRow.children.forEachIndexed { index, child ->
            val method = methods[index]
            val methodView = child as LinearLayout
            methodView.findViewById<ImageView>(R.id.imgMethod).setImageResource(method.iconRes)
            methodView.findViewById<TextView>(R.id.tvMethod).text = method.title
            methodView.setOnClickListener {
                selectedMethod = index
                updateSelectedMethod(binding)
                openSelectedUnlockMethod()
            }
        }
    }

    private fun openSelectedUnlockMethod() {
        when (selectedMethod) {
            0 -> showPinUnlock()
            1 -> showPatternUnlock()
            2 -> startVoiceUnlock()
            3 -> openActivityPreview()
        }
    }

    private fun updateSelectedMethod(binding: FragmentScreenPreviewBinding) {
        binding.methodsRow.children.forEachIndexed { index, child ->
            val methodView = child as LinearLayout
            val icon = methodView.findViewById<ImageView>(R.id.imgMethod)
            val label = methodView.findViewById<TextView>(R.id.tvMethod)
            val selected = index == selectedMethod

            methodView.setBackgroundResource(
                if (selected) R.drawable.bg_preview_method_selected else R.drawable.bg_preview_method
            )
            icon.setBackgroundResource(
                if (selected) R.drawable.bg_preview_method_icon_selected else R.drawable.bg_preview_method_icon
            )
            icon.setColorFilter(
                ContextCompat.getColor(context, if (selected) R.color.blue_primary else R.color.white)
            )
            label.setTextColor(
                ContextCompat.getColor(context, if (selected) R.color.blue_primary else R.color.white)
            )
        }

        binding.tvSpeakHint.text = when (selectedMethod) {
            0 -> context.getString(R.string.pin_unlock)
            1 -> "Draw your pattern to unlock"
            2 -> context.getString(R.string.tap_microphone)
            else -> "Touch fingerprint sensor"
        }
    }

    private fun showPinUnlock() {
        if (!SecurePinStore.hasPin(context)) {
            Toast.makeText(context, R.string.pin_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = addDialogView(R.layout.dialog_pin_unlock)
        val dots = dialog.findViewById<LinearLayout>(R.id.pinDotsContainer)
        val pinLength = SecurePinStore.getPinLength(context)
        val enteredPin = StringBuilder()
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

        setupPinDots(dots, pinLength, enteredPin.length)
        buttons.forEach { (id, digit) ->
            dialog.findViewById<View>(id).setOnClickListener {
                if (enteredPin.length < pinLength) {
                    enteredPin.append(digit)
                    setupPinDots(dots, pinLength, enteredPin.length)
                }
            }
        }
        dialog.findViewById<View>(R.id.btnBackspace).setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.lastIndex)
                setupPinDots(dots, pinLength, enteredPin.length)
            }
        }
        dialog.findViewById<View>(R.id.btnConfirm).setOnClickListener {
            if (enteredPin.length != pinLength) {
                Toast.makeText(context, R.string.pin_incomplete, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (SecurePinStore.verifyPin(context, enteredPin.toString())) {
                Toast.makeText(context, R.string.pin_unlocked, Toast.LENGTH_SHORT).show()
                dismiss(unlocked = true)
            } else {
                enteredPin.clear()
                setupPinDots(dots, pinLength, enteredPin.length)
                binding?.tvSpeakHint?.text = context.getString(R.string.pin_wrong)
                Toast.makeText(context, R.string.pin_wrong, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupPinDots(container: LinearLayout, pinLength: Int, enteredLength: Int) {
        container.removeAllViews()
        repeat(pinLength) { index ->
            val dot = View(themedContext)
            dot.background = ContextCompat.getDrawable(
                context,
                if (index < enteredLength) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot_empty
            )
            container.addView(dot, LinearLayout.LayoutParams(16.dp, 16.dp).apply {
                marginStart = 8.dp
                marginEnd = 8.dp
            })
        }
    }

    private fun showPatternUnlock() {
        if (!SecurePatternStore.hasPattern(context)) {
            Toast.makeText(context, R.string.pattern_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = addDialogView(R.layout.dialog_pattern_unlock)
        val patternView = dialog.findViewById<PatternLockView>(R.id.patternView)
        val status = dialog.findViewById<TextView>(R.id.tvPatternStatus)
        patternView.onPatternComplete = { sequence ->
            if (SecurePatternStore.verifyPattern(context, sequence)) {
                Toast.makeText(context, R.string.pattern_unlocked, Toast.LENGTH_SHORT).show()
                dismiss(unlocked = true)
            } else {
                status.text = context.getString(R.string.pattern_wrong)
                binding?.tvSpeakHint?.text = context.getString(R.string.pattern_wrong)
                Toast.makeText(context, R.string.pattern_wrong, Toast.LENGTH_SHORT).show()
                patternView.clearPattern()
            }
        }
    }

    private fun startVoiceUnlock() {
        val savedPhrase = context
            .getSharedPreferences(VoiceSetupFragment.VOICE_PREFS, Context.MODE_PRIVATE)
            .getString(VoiceSetupFragment.KEY_VOICE_PHRASE_NORMALIZED, "")
            .orEmpty()

        if (savedPhrase.isBlank()) {
            Toast.makeText(context, R.string.save_voice_first, Toast.LENGTH_SHORT).show()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, R.string.speech_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        stopRecognizer()
        binding?.tvSpeakHint?.text = context.getString(R.string.listening_unlock)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    binding?.tvSpeakHint?.text = context.getString(R.string.listening_unlock)
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    showVoiceTryAgain()
                }

                override fun onResults(results: Bundle?) {
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (VoiceSetupFragment.normalizePhrase(spoken) == savedPhrase) {
                        Toast.makeText(context, R.string.voice_unlocked, Toast.LENGTH_SHORT).show()
                        dismiss(unlocked = true)
                    } else {
                        showVoiceTryAgain()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            startListening(speechIntent())
        }
    }

    private fun showVoiceTryAgain() {
        binding?.tvSpeakHint?.text = context.getString(R.string.voice_try_again)
        Toast.makeText(context, R.string.voice_try_again, Toast.LENGTH_SHORT).show()
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

    private fun addDialogView(layoutRes: Int): View {
        val root = container ?: error("Overlay is not attached")
        root.findViewWithTag<View>(DIALOG_TAG)?.let { root.removeView(it) }

        val dim = FrameLayout(themedContext).apply {
            tag = DIALOG_TAG
            setBackgroundColor(0x99000000.toInt())
            isClickable = true
        }
        val dialog = inflater.inflate(layoutRes, dim, false)
        dim.addView(
            dialog,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                leftMargin = 28.dp
                rightMargin = 28.dp
            }
        )
        root.addView(
            dim,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        return dialog
    }

    private fun openActivityPreview() {
        val intent = Intent(context, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(HomeActivity.EXTRA_SHOW_LOCK_PREVIEW, true)
        }
        context.startActivity(intent)
    }

    private fun overlayLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun canDrawOverlays(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).toInt()

    private data class PreviewMethod(
        val title: String,
        val iconRes: Int
    )

    companion object {
        private const val DIALOG_TAG = "lock_dialog"
    }
}
