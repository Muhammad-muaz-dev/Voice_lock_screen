package com.futurecode.voicelockscreen.utils

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ViewLockScreenBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LockMethod(@StringRes val hint: Int) {
    PIN(R.string.pin_unlock),
    PATTERN(R.string.draw_pattern_to_unlock),
    VOICE(R.string.tap_microphone),
    FINGERPRINT(R.string.touch_fingerprint_sensor)
}

object LockScreenUi {
    fun bind(
        binding: ViewLockScreenBinding,
        quote: String? = null,
        showClockAndQuote: Boolean? = null,
        onUnlock: ((LockMethod) -> Unit)? = null
    ) {
        val context = binding.root.context
        val now = Date()
        binding.tvMainTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        binding.tvDate.text = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now)
        binding.tvQuote.text = quote ?: VoiceLockFeatureStore.lockQuote(context)
        binding.tvGreeting.setText(context.getTimeBasedGreetingRes())
        val show = showClockAndQuote ?: VoiceLockFeatureStore.showClockAndQuote(context)
        binding.timeCard.isVisible = show
        binding.quoteCard.isVisible = show
        binding.tvGreeting.isVisible = show

        if (onUnlock != null) {
            binding.lockHelp.setOnClickListener {
                Toast.makeText(context, R.string.lock_screen_help_message, Toast.LENGTH_LONG).show()
            }
            listOf(
                binding.methodPin to LockMethod.PIN,
                binding.methodPattern to LockMethod.PATTERN,
                binding.methodFingerprint to LockMethod.FINGERPRINT
            ).forEach { (button, method) ->
                button.isClickable = true
                button.isFocusable = true
                button.importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
                button.setOnClickListener { onUnlock(method) }
            }
            binding.micRings.setOnClickListener { onUnlock(LockMethod.VOICE) }
            val minTop = context.resources.getDimensionPixelSize(R.dimen.spacing_xxxl)
            val minBottom = context.resources.getDimensionPixelSize(R.dimen.spacing_xxl)
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
                val safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                binding.lockContent.updatePadding(
                    left = safe.left, top = maxOf(minTop, safe.top),
                    right = safe.right, bottom = maxOf(minBottom, safe.bottom)
                )
                insets
            }
            ViewCompat.requestApplyInsets(binding.root)
        } else {
            // Wallpaper previews are visual only. Keep the controls visible and the viewport scrollable.
            listOf(binding.lockHelp, binding.methodPin, binding.methodPattern, binding.methodFingerprint, binding.micRings)
                .forEach { it.isClickable = false; it.isFocusable = false }
        }
    }

    fun select(binding: ViewLockScreenBinding, method: LockMethod) {
        binding.methodPin.isSelected = method == LockMethod.PIN
        binding.methodPattern.isSelected = method == LockMethod.PATTERN
        binding.methodFingerprint.isSelected = method == LockMethod.FINGERPRINT
        binding.tvSpeakHint.setText(method.hint)
    }
}
