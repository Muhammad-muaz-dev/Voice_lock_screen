package com.futurecode.voicelockscreen.utils

import android.content.res.ColorStateList
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.content.ContextCompat
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ViewPinEntryBinding
import com.futurecode.voicelockscreen.databinding.DialogSecurityRecoveryBinding
import com.futurecode.voicelockscreen.ui.afterlogin.SecurityQuestionFragment
import com.futurecode.voicelockscreen.ui.afterlogin.VoiceSetupFragment

object CredentialUnlockUi {
    private const val RECOVERY_ATTEMPTS = "recovery_failed_attempts"
    private const val RECOVERY_LOCKOUT = "recovery_lockout_until"

    fun showRecovery(context: Context, showOverlay: (Int) -> View, onVerified: () -> Unit) {
        val prefs = context.getSharedPreferences(VoiceSetupFragment.VOICE_PREFS, Context.MODE_PRIVATE)
        val question = prefs.getString(SecurityQuestionFragment.PREF_QUESTION, null).orEmpty()
        val savedAnswer = prefs.getString(SecurityQuestionFragment.PREF_ANSWER, null).orEmpty()
        if (question.isBlank() || savedAnswer.isBlank()) {
            Toast.makeText(context, R.string.lock_screen_help_message, Toast.LENGTH_LONG).show()
            return
        }

        val recovery = DialogSecurityRecoveryBinding.bind(showOverlay(R.layout.dialog_security_recovery))
        recovery.recoveryQuestion.text = question
        recovery.recoveryAnswer.doAfterTextChanged { answer ->
            recovery.recoveryApply.isEnabled = !answer.isNullOrBlank()
            recovery.recoveryApply.alpha = if (recovery.recoveryApply.isEnabled) 1f else 0.5f
            recovery.recoveryAnswer.error = null
        }
        recovery.recoveryApply.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now < prefs.getLong(RECOVERY_LOCKOUT, 0L)) {
                recovery.recoveryAnswer.error = context.getString(R.string.lockout_active)
                return@setOnClickListener
            }
            val answer = recovery.recoveryAnswer.text.toString().trim()
            if (answer == savedAnswer) {
                prefs.edit().remove(RECOVERY_ATTEMPTS).remove(RECOVERY_LOCKOUT).apply()
                recovery.recoveryAnswer.text?.clear()
                onVerified()
            } else {
                val attempts = prefs.getInt(RECOVERY_ATTEMPTS, 0) + 1
                val locked = attempts >= 5
                prefs.edit().putInt(RECOVERY_ATTEMPTS, if (locked) 0 else attempts)
                    .putLong(RECOVERY_LOCKOUT, if (locked) now + 30 * 60_000L else 0L)
                    .apply()
                recovery.recoveryAnswer.error = context.getString(
                    if (locked) R.string.lockout_active else R.string.recovery_answer_incorrect
                )
            }
        }
    }
    fun bindPin(view: View, pinLength: Int, onSubmit: (String) -> Unit) {
        val entry = ViewPinEntryBinding.bind(view.findViewById(R.id.unlockPinEntry))
        val input = StringBuilder()
        val resources = view.resources
        val white = ContextCompat.getColor(view.context, R.color.white)
        val blue = ContextCompat.getColor(view.context, R.color.primary)
        val ink = ContextCompat.getColor(view.context, R.color.textPrimary)
        val keyStates = arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf())
        val keyFill = ColorStateList(keyStates, intArrayOf(blue, white))
        val keyText = ColorStateList(keyStates, intArrayOf(white, ink))
        val shadow = ContextCompat.getColor(view.context, R.color.scrimStrong)
        listOf(entry.pinPrompt, entry.clearPin).forEach { label ->
            label.setTextColor(white)
            label.setShadowLayer(resources.displayMetrics.density * 2f, 0f, 0f, shadow)
        }
        entry.entryKeyDelete.imageTintList = ColorStateList.valueOf(white)
        entry.pinInfo.isVisible = false
        entry.pinLengths.isVisible = false
        entry.pinPrompt.setText(R.string.pin_please_enter)

        val size = resources.getDimensionPixelSize(R.dimen.credential_pin_dot_size)
        val margin = resources.getDimensionPixelSize(R.dimen.spacing_sm)
        repeat(pinLength) {
            entry.pinEntryDots.addView(View(view.context), LinearLayout.LayoutParams(size, size).apply {
                marginStart = margin
                marginEnd = margin
            })
        }
        fun renderDots() {
            entry.pinEntryDots.children.forEachIndexed { index, dot ->
                dot.setBackgroundResource(
                    if (index < input.length) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot_empty
                )
            }
        }
        renderDots()
        listOf(
            entry.entryKey0, entry.entryKey1, entry.entryKey2, entry.entryKey3, entry.entryKey4,
            entry.entryKey5, entry.entryKey6, entry.entryKey7, entry.entryKey8, entry.entryKey9
        ).forEachIndexed { digit, key ->
            key.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(keyFill)
            }
            key.setTextColor(keyText)
            key.setOnClickListener {
                input.append(digit)
                val pin = input.toString().takeIf { it.length == pinLength }
                if (pin != null) input.clear()
                renderDots()
                pin?.let(onSubmit)
            }
        }
        entry.entryKeyDelete.setOnClickListener {
            if (input.isNotEmpty()) input.deleteCharAt(input.lastIndex)
            renderDots()
        }
        entry.clearPin.setOnClickListener {
            input.clear()
            renderDots()
        }
    }

    fun styleOverlay(overlay: View, lockContent: View) {
        overlay.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                lockContent.visibility = View.INVISIBLE
            }

            override fun onViewDetachedFromWindow(view: View) {
                lockContent.visibility = View.VISIBLE
                view.removeOnAttachStateChangeListener(this)
            }
        })
        ViewCompat.setOnApplyWindowInsetsListener(overlay) { view, insets ->
            val safe = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = safe.left, top = safe.top, right = safe.right, bottom = safe.bottom)
            insets
        }
    }

    fun requestInsets(overlay: View) = ViewCompat.requestApplyInsets(overlay)
}
