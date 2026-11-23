package com.futurecode.voicelockscreen.adapter

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.futurecode.voicelockscreen.R

class WallpaperShimmerView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val base = ContextCompat.getColor(context, R.color.surfaceTint)
    private val highlight = ContextCompat.getColor(context, R.color.surface)
    private val paint = Paint()
    private val translation = Matrix()
    private val sweep = ValueAnimator.ofFloat(-1f, 1f).apply {
        duration = 1400L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        paint.shader = if (w > 0) {
            LinearGradient(
                0f, 0f, w.toFloat(), h * 0.25f,
                intArrayOf(base, highlight, base), null, Shader.TileMode.CLAMP
            )
        } else null
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible && !sweep.isStarted) sweep.start()
        if (!isVisible) sweep.cancel()
    }

    override fun onDetachedFromWindow() {
        sweep.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(base)
        if (!sweep.isRunning) return
        translation.setTranslate(width * (sweep.animatedValue as Float), 0f)
        paint.shader?.setLocalMatrix(translation)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}