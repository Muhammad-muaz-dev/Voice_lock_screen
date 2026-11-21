package com.futurecode.voicelockscreen.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.futurecode.voicelockscreen.R
import kotlin.math.min

/**
 * Voice enrolment ring: a soft green track, a green progress arc with round caps,
 * and a shield-check badge in the middle. Progress changes animate.
 */
class CameraProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val strokeWidthPx = resources.displayMetrics.density * STROKE_DP

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = ContextCompat.getColor(context, R.color.successSoft)
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.successStrong)
    }

    private val badge: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_camera_shield_check)
    private val arcBounds = RectF()
    private var shownProgress = 0f
    private var animator: ValueAnimator? = null

    /** Sets progress (0f..1f), animating from the value currently shown. */
    fun setProgress(target: Float, animate: Boolean = true) {
        val clamped = target.coerceIn(0f, 1f)
        animator?.cancel()
        if (!animate) {
            shownProgress = clamped
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(shownProgress, clamped).apply {
            duration = ANIMATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                shownProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val inset = strokeWidthPx / 2f
        val left = (width - size) / 2f + inset
        val top = (height - size) / 2f + inset
        arcBounds.set(left, top, left + size - strokeWidthPx, top + size - strokeWidthPx)

        canvas.drawOval(arcBounds, trackPaint)
        if (shownProgress > 0f) {
            canvas.drawArc(arcBounds, START_ANGLE, 360f * shownProgress, false, arcPaint)
        }

        badge?.let {
            val badgeSize = (size * BADGE_RATIO).toInt()
            val cx = width / 2
            val cy = height / 2
            it.setBounds(cx - badgeSize / 2, cy - badgeSize / 2, cx + badgeSize / 2, cy + badgeSize / 2)
            it.draw(canvas)
        }
    }

    private companion object {
        const val STROKE_DP = 7f
        const val START_ANGLE = -90f
        const val BADGE_RATIO = 0.5f
        const val ANIMATION_MS = 600L
    }
}
