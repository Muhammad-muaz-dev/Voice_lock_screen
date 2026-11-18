package com.futurecode.voicelockscreen.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import com.futurecode.voicelockscreen.R
import java.text.NumberFormat
import kotlin.math.roundToInt

/**
 * Brand progress bar for the splash: a rounded translucent track, a violet-to-cyan
 * gradient fill with a moving shimmer, and a glowing knob at the leading edge.
 * The shimmer is skipped when the system has animations turned off.
 */
class VoiceProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Fraction filled, 0f..1f. */
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            ViewCompat.setStateDescription(this, percentText())
            invalidate()
        }

    private val startColor = ContextCompat.getColor(context, R.color.primary)
    private val endColor = ContextCompat.getColor(context, R.color.accentCyan)
    private val knobColor = ContextCompat.getColor(context, R.color.text_white)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(knobColor, TRACK_ALPHA)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = knobColor }

    private val trackRect = RectF()
    private val fillRect = RectF()
    private val fillClip = Path()
    private val percentFormat = NumberFormat.getPercentInstance()

    private var shimmerOffset = 0f
    private val shimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = SHIMMER_DURATION_MS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            shimmerOffset = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        ViewCompat.setStateDescription(this, percentText())
    }

    override fun getAccessibilityClassName(): CharSequence = ProgressBar::class.java.name

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val thickness = h * TRACK_THICKNESS_RATIO
        val inset = h / 2f
        trackRect.set(inset, (h - thickness) / 2f, w - inset, (h + thickness) / 2f)
        fillPaint.shader = LinearGradient(
            trackRect.left, 0f, trackRect.right, 0f,
            startColor, endColor, Shader.TileMode.CLAMP
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ValueAnimator.areAnimatorsEnabled()) shimmerAnimator.start()
    }

    override fun onDetachedFromWindow() {
        shimmerAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = trackRect.height() / 2f
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint)

        val fillEnd = trackRect.left + trackRect.width() * progress
        if (fillEnd <= trackRect.left) return
        fillRect.set(trackRect.left, trackRect.top, fillEnd, trackRect.bottom)
        canvas.drawRoundRect(fillRect, radius, radius, fillPaint)
        drawShimmer(canvas, radius)
        drawKnob(canvas, fillEnd)
    }

    private fun drawShimmer(canvas: Canvas, radius: Float) {
        if (!shimmerAnimator.isRunning) return
        val band = trackRect.width() * SHIMMER_BAND_RATIO
        val bandStart = trackRect.left - band + (trackRect.width() + band) * shimmerOffset
        val highlight = ColorUtils.setAlphaComponent(knobColor, SHIMMER_ALPHA)
        shimmerPaint.shader = LinearGradient(
            bandStart, 0f, bandStart + band, 0f,
            intArrayOf(Color.TRANSPARENT, highlight, Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
        fillClip.reset()
        fillClip.addRoundRect(fillRect, radius, radius, Path.Direction.CW)
        val save = canvas.save()
        canvas.clipPath(fillClip)
        canvas.drawRect(fillRect, shimmerPaint)
        canvas.restoreToCount(save)
    }

    private fun drawKnob(canvas: Canvas, x: Float) {
        val y = trackRect.centerY()
        val glowRadius = height / 2f
        glowPaint.shader = RadialGradient(
            x, y, glowRadius,
            ColorUtils.setAlphaComponent(endColor, GLOW_ALPHA),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, glowRadius, glowPaint)
        canvas.drawCircle(x, y, trackRect.height() * KNOB_RATIO, knobPaint)
    }

    /** Rounded percentage for the label next to the bar. */
    fun percentText(): String = percentFormat.format((progress * 100).roundToInt() / 100f)

    private companion object {
        const val TRACK_THICKNESS_RATIO = 0.4f
        const val TRACK_ALPHA = 36
        const val KNOB_RATIO = 0.75f
        const val GLOW_ALPHA = 150
        const val SHIMMER_ALPHA = 110
        const val SHIMMER_BAND_RATIO = 0.35f
        const val SHIMMER_DURATION_MS = 1100L
    }
}
