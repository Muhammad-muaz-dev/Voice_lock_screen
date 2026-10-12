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
import androidx.core.view.children
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.LockScreenActivity
import com.futurecode.voicelockscreen.databinding.FragmentScreenPreviewBinding
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.ui.custom.PatternLockView
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore
import com.futurecode.voicelockscreen.utils.VoiceLockPattern
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LockOverlayWindow(private val context: Context) {
    private val themedContext = ContextThemeWrapper(context, R.style.Theme_VoiceLockScreen)
    private val inflater = LayoutInflater.from(themedContext)
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var root: FrameLayout? = null
    private var binding: FragmentScreenPreviewBinding? = null
    private var selectedMethod = VOICE_INDEX
    private var isUnlocking = false

    private val methods = listOf(
        PreviewMethod(R.string.pin, R.drawable.ic_pin_lock),
        PreviewMethod(R.string.pattern, R.drawable.ic_pattern_lock),
        PreviewMethod(R.string.voice, R.drawable.ic_mic_blue),
        PreviewMethod(R.string.fingerprint, R.drawable.ic_fingerprint_lock)
    )

    fun show() {
        if (!canDrawOverlays() || root != null) return

        val overlayRoot = FrameLayout(themedContext)
        val overlayBinding = FragmentScreenPreviewBinding.inflate(inflater, overlayRoot, false)
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
        bindDateTime(overlayBinding)
        ThemeWallpaperStore.applyContent(overlayBinding, context)
        bindMethods(overlayBinding)
        updateSelectedMethod(overlayBinding)
        overlayBinding.micRings.setOnClickListener { openSelectedMethod() }
    }

    fun dismiss(unlocked: Boolean = false) {
        if (unlocked) AppLockState.setUnlockedThisSession(context, true)
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        binding = null
        isUnlocking = false
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
            val container = child as LinearLayout
            container.findViewById<ImageView>(R.id.imgMethod).setImageResource(method.iconRes)
            container.findViewById<TextView>(R.id.tvMethod).setText(method.titleRes)
            container.setOnClickListener {
                selectedMethod = index
                updateSelectedMethod(binding)
                openSelectedMethod()
            }
        }
    }

    private fun updateSelectedMethod(binding: FragmentScreenPreviewBinding) {
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
            icon.setColorFilter(ContextCompat.getColor(context, if (selected) R.color.blue_primary else R.color.white))
            label.setTextColor(ContextCompat.getColor(context, if (selected) R.color.blue_primary else R.color.white))
        }
        binding.tvSpeakHint.text = when (selectedMethod) {
            PIN_INDEX -> context.getString(R.string.pin_unlock)
            PATTERN_INDEX -> context.getString(R.string.draw_pattern_to_unlock)
            VOICE_INDEX -> context.getString(R.string.tap_microphone)
            else -> context.getString(R.string.touch_fingerprint_sensor)
        }
    }

    private fun openSelectedMethod() {
        when (selectedMethod) {
            PIN_INDEX -> showPinFallback()
            PATTERN_INDEX -> showPatternFallback()
            VOICE_INDEX, FINGERPRINT_INDEX -> openLockActivity()
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
            context.startActivity(LockScreenActivity.newIntent(context))
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

    private data class PreviewMethod(
        val titleRes: Int,
        val iconRes: Int
    )

    companion object {
        private const val PIN_INDEX = 0
        private const val PATTERN_INDEX = 1
        private const val VOICE_INDEX = 2
        private const val FINGERPRINT_INDEX = 3
        private const val FALLBACK_TAG = "lock_fallback"
    }
}

