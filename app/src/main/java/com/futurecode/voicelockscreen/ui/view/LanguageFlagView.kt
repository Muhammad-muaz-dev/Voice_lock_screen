package com.futurecode.voicelockscreen.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

/** System flag glyphs clipped into circles for the flat language list. */
class LanguageFlagView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = Rect()
    private val clip = Path()
    var country: String = "GB"
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        val x = width / 2f
        val y = height / 2f
        clip.reset()
        clip.addCircle(x, y, size / 2, Path.Direction.CW)
        val circleSave = canvas.save()
        canvas.clipPath(clip)
        drawFlag(canvas, country, x, y, size)

        canvas.restoreToCount(circleSave)
    }

    private fun drawFlag(canvas: Canvas, region: String, x: Float, y: Float, size: Float) {
        val glyph = region.uppercase(java.util.Locale.ROOT).map {
            String(Character.toChars(0x1F1E6 + it.code - 'A'.code))
        }.joinToString("")
        paint.textSize = size * 1.2f
        paint.getTextBounds(glyph, 0, glyph.length, bounds)
        val textWidth = paint.measureText(glyph)
        canvas.drawText(glyph, x - textWidth / 2, y - bounds.exactCenterY(), paint)
    }
}
