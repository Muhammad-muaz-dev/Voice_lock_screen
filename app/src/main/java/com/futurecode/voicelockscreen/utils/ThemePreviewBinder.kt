package com.futurecode.voicelockscreen.utils

import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.futurecode.voicelockscreen.R

object ThemePreviewBinder {
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
