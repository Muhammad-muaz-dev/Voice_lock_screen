package com.futurecode.voicelockscreen.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
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
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.LockScreenActivity
import com.futurecode.voicelockscreen.databinding.ViewLockScreenBinding
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.ui.custom.PatternLockView
import com.futurecode.voicelockscreen.utils.LockMethod
import com.futurecode.voicelockscreen.utils.LockScreenUi
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore
import com.futurecode.voicelockscreen.utils.VoiceLockPattern

class LockOverlayWindow(private val context: Context) {
    private val themedContext = ContextThemeWrapper(context, R.style.Theme_VoiceLockScreen)
    private val inflater = LayoutInflater.from(themedContext)
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var root: FrameLayout? = null
    private var binding: ViewLockScreenBinding? = null
    private var selectedMethod = LockMethod.VOICE
    private var isUnlocking = false


    fun show() {
        if (!canDrawOverlays() || root != null) return

        val overlayRoot = FrameLayout(themedContext)
        val overlayBinding = ViewLockScreenBinding.inflate(inflater, overlayRoot, false)
        overlayRoot.addView(
            overlayBinding.root,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        root = overlayRoot
        binding = overlayBinding
        windowManager.addView(overlayRoot, layoutParams())

        ThemeWallpaperStore.applyTo(overlayBinding.imgWallpaper, context)
        LockScreenUi.bind(overlayBinding) { method ->
            selectedMethod = method
            LockScreenUi.select(overlayBinding, method)
            openSelectedMethod()
        }
        LockScreenUi.select(overlayBinding, selectedMethod)
    }

    fun dismiss(unlocked: Boolean = false) {
        if (unlocked) AppLockState.setUnlockedThisSession(context, true)
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        binding = null
        isUnlocking = false
    }

    private fun openSelectedMethod() {
        when (selectedMethod) {
            LockMethod.PIN -> showPinFallback()
            LockMethod.PATTERN -> showPatternFallback()
            LockMethod.VOICE, LockMethod.FINGERPRINT -> openLockActivity()
        }
    }

    private fun showPinFallback() {
        if (!SecurePinStore.hasPin(context)) {
            Toast.makeText(context, R.string.pin_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        val fallback = addFallbackView(R.layout.dialog_pin_unlock)
        val dots = fallback.findViewById<LinearLayout>(R.id.pinDotsContainer)
        val enteredPin = StringBuilder()
        val pinLength = SecurePinStore.getPinLength(context)
        setupPinDots(dots, pinLength, enteredPin.length)

        listOf(
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
        ).forEach { (id, digit) ->
            fallback.findViewById<View>(id).setOnClickListener {
                if (enteredPin.length < pinLength) {
                    enteredPin.append(digit)
                    setupPinDots(dots, pinLength, enteredPin.length)
                }
            }
        }
        fallback.findViewById<View>(R.id.btnBackspace).setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.lastIndex)
                setupPinDots(dots, pinLength, enteredPin.length)
            }
        }
        fallback.findViewById<View>(R.id.btnConfirm).setOnClickListener {
            if (isUnlocking) return@setOnClickListener
            if (enteredPin.length != pinLength) {
                Toast.makeText(context, R.string.pin_incomplete, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (SecurePinStore.verifyPin(context, enteredPin.toString())) {
                isUnlocking = true
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

    private fun showPatternFallback() {
        if (!SecurePatternStore.hasPattern(context)) {
            Toast.makeText(context, R.string.pattern_setup_required, Toast.LENGTH_SHORT).show()
            return
        }

        val fallback = addFallbackView(R.layout.dialog_pattern_unlock)
        val patternView = fallback.findViewById<VoiceLockPattern>(R.id.patternView)
        val status = fallback.findViewById<TextView>(R.id.tvPatternStatus)
        patternView.onPatternComplete = patternComplete@{ sequence ->
            if (isUnlocking) return@patternComplete
            if (SecurePatternStore.verifyPattern(context, sequence)) {
                isUnlocking = true
                patternView.onPatternComplete = null
                dismiss(unlocked = true)
            } else {
                status.text = context.getString(R.string.pattern_wrong)
                binding?.tvSpeakHint?.text = context.getString(R.string.pattern_wrong)
                Toast.makeText(context, R.string.pattern_wrong, Toast.LENGTH_SHORT).show()
                patternView.clearPattern()
            }
        }
    }

    private fun addFallbackView(layoutRes: Int): View {
        val overlayRoot = root ?: error("Lock overlay is not attached")
        overlayRoot.findViewWithTag<View>(FALLBACK_TAG)?.let { overlayRoot.removeView(it) }
        val dim = FrameLayout(themedContext).apply {
            tag = FALLBACK_TAG
            setBackgroundColor(ContextCompat.getColor(context, R.color.bg_color))
            isClickable = true
            isFocusable = true
        }
        val fallback = inflater.inflate(layoutRes, dim, false)
        dim.addView(
            fallback,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        dim.addView(createFallbackBackButton())
        overlayRoot.addView(
            dim,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        return fallback
    }

    private fun createFallbackBackButton(): ImageView {
        return ImageView(themedContext).apply {
            setBackgroundResource(R.drawable.bg_circle_white)
            setImageResource(R.drawable.ic_back_blue)
            contentDescription = context.getString(R.string.back)
            setPadding(11.dp, 11.dp, 11.dp, 11.dp)
            elevation = 8.dp.toFloat()
            setOnClickListener { removeFallbackView() }
            layoutParams = FrameLayout.LayoutParams(42.dp, 42.dp, Gravity.TOP or Gravity.START).apply {
                marginStart = 20.dp
                topMargin = 20.dp
            }
        }
    }

    private fun removeFallbackView() {
        val overlayRoot = root ?: return
        overlayRoot.findViewWithTag<View>(FALLBACK_TAG)?.let { overlayRoot.removeView(it) }
    }

    private fun openLockActivity() {
        dismiss()
        runCatching {
            context.startActivity(LockScreenActivity.newIntent(context, fingerprint = selectedMethod == LockMethod.FINGERPRINT))
        }
    }

    private fun layoutParams(): WindowManager.LayoutParams {
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


    companion object {
        private const val FALLBACK_TAG = "lock_fallback"
    }
}
