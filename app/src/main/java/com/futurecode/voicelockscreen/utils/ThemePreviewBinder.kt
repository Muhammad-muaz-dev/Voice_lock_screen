package com.futurecode.voicelockscreen.utils

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.FragmentThemePreviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Fills the reference-style lock preview (time card, greeting, quote) shared by theme, quote and upload previews. */
object ThemePreviewBinder {
    fun bindContent(binding: FragmentThemePreviewBinding, context: Context, quote: String) {
        val now = Date()
        binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        binding.tvDate.text = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now)
        binding.tvPartOfDay.text = context.getTimeBasedGreetingRes()
        binding.tvQuote.text = quote
    }

    /** Reference img_quote uses a left-to-right gradient text; ours runs through the violet family. */
    fun applySampleQuoteGradient(text: TextView) {
        text.doOnLayout {
            val start = ContextCompat.getColor(text.context, R.color.vn_primary02)
            val end = ContextCompat.getColor(text.context, R.color.vn_primary05)
            text.paint.shader = LinearGradient(0f, 0f, text.width.toFloat(), 0f, start, end, Shader.TileMode.CLAMP)
            text.invalidate()
        }
    }
}
