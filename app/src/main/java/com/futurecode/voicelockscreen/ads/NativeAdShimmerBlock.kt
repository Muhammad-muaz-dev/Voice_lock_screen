package com.futurecode.voicelockscreen.ads

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.futurecode.voicelockscreen.R

/** A lightweight animated skeleton block used in native-ad loading placeholders. */
class NativeAdShimmerBlock @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val baseColor = ContextCompat.getColor(context, R.color.surfaceDisabled)
    private val highlightColor = ContextCompat.getColor(context, R.color.surface)
    private val cornerRadius = context.obtainStyledAttributes(
        attrs,
        R.styleable.NativeAdShimmerBlock
    ).let { array ->
        val value = array.getDimension(
            R.styleable.NativeAdShimmerBlock_shimmerCornerRadius,
            resources.getDimension(R.dimen.radius_xs)
        )
        array.recycle()
        value
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private val shaderMatrix = Matrix()
    private var gradient: LinearGradient? = null
    private var shimmerOffset = 0f
    private var sweepWidth = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = SHIMMER_DURATION_MS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            shimmerOffset = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        sweepWidth = (width * SWEEP_WIDTH_RATIO).coerceAtLeast(resources.getDimension(R.dimen.spacing_xxl))
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        gradient = LinearGradient(
            0f,
            0f,
            sweepWidth,
            0f,
            intArrayOf(baseColor, highlightColor, baseColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        startShimmerIfVisible()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startShimmerIfVisible()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint.apply { color = baseColor })

        val shader = gradient ?: return
        val travel = width + sweepWidth
        shaderMatrix.setTranslate(-sweepWidth + travel * shimmerOffset, 0f)
        shader.setLocalMatrix(shaderMatrix)
        paint.shader = shader
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint)
        paint.shader = null
    }

    private fun startShimmerIfVisible() {
        if (
            animationsEnabled() &&
            isAttachedToWindow &&
            width > 0 &&
            !animator.isStarted
        ) {
            animator.start()
        }
    }

    private fun animationsEnabled(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()

    private companion object {
        const val SHIMMER_DURATION_MS = 1100L
        const val SWEEP_WIDTH_RATIO = 0.7f
    }
}
