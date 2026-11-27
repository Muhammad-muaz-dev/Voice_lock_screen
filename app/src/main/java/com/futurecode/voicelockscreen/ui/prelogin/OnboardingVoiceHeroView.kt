package com.futurecode.voicelockscreen.ui.prelogin

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import com.futurecode.voicelockscreen.R

/** Runs the voice illustration's subtle loops only while its onboarding page is active. */
class OnboardingVoiceHeroView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var pageIsActive = false
    private var animationSet: AnimatorSet? = null

    fun setAnimationActive(active: Boolean) {
        pageIsActive = active
        updateAnimationState()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimationState()
    }

    override fun onDetachedFromWindow() {
        stopAnimations()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimationState()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateAnimationState()
    }

    private fun updateAnimationState() {
        val shouldAnimate = pageIsActive && isAttachedToWindow && isShown && !isInEditMode
        if (shouldAnimate) startAnimations() else stopAnimations()
    }

    private fun startAnimations() {
        if (animationSet?.isRunning == true) return

        val phone = findViewById<View>(R.id.onboardPhone)
        val phraseBubble = findViewById<View>(R.id.voicePhraseBubble)
        val micOrb = findViewById<View>(R.id.voiceMicOrb)
        val micIcon = findViewById<View>(R.id.voiceMicIcon)
        val outerRing = findViewById<View>(R.id.voicePulseRingOuter)
        val innerRing = findViewById<View>(R.id.voicePulseRingInner)
        val waveBars = listOf(
            R.id.voiceWaveBar1,
            R.id.voiceWaveBar2,
            R.id.voiceWaveBar3,
            R.id.voiceWaveBar4,
            R.id.voiceWaveBar5
        ).map { id -> findViewById<View>(id) }

        val animations = mutableListOf<Animator>(
            loop(phone, "translationY", 2_600L, 0L, 0f, -dp(4), 0f),
            loop(phraseBubble, "translationY", 2_100L, 150L, 0f, -dp(3), 0f),
            loop(micOrb, "scaleX", 1_650L, 0L, 1f, 1.045f, 1f),
            loop(micOrb, "scaleY", 1_650L, 0L, 1f, 1.045f, 1f),
            loop(micIcon, "scaleX", 1_650L, 0L, 1f, 1.045f, 1f),
            loop(micIcon, "scaleY", 1_650L, 0L, 1f, 1.045f, 1f),
            pulse(outerRing, 1_900L, 0L),
            pulse(innerRing, 1_900L, 640L)
        )

        waveBars.forEachIndexed { index, bar ->
            animations += loop(
                bar,
                "scaleY",
                620L,
                index * 90L,
                0.48f,
                1.18f,
                0.62f,
                1f
            )
        }

        animationSet = AnimatorSet().apply {
            playTogether(animations)
            start()
        }
    }

    private fun stopAnimations() {
        animationSet?.cancel()
        animationSet = null

        findViewById<View>(R.id.onboardPhone)?.translationY = 0f
        findViewById<View>(R.id.voicePhraseBubble)?.translationY = 0f
        findViewById<View>(R.id.voiceMicOrb)?.apply {
            scaleX = 1f
            scaleY = 1f
        }
        findViewById<View>(R.id.voiceMicIcon)?.apply {
            scaleX = 1f
            scaleY = 1f
        }
        findViewById<View>(R.id.voicePulseRingOuter)?.resetRing(alpha = 0.35f)
        findViewById<View>(R.id.voicePulseRingInner)?.resetRing(alpha = 0.5f)
        listOf(
            R.id.voiceWaveBar1,
            R.id.voiceWaveBar2,
            R.id.voiceWaveBar3,
            R.id.voiceWaveBar4,
            R.id.voiceWaveBar5
        ).forEach { findViewById<View>(it)?.scaleY = 1f }
    }

    private fun View.resetRing(alpha: Float) {
        this.alpha = alpha
        scaleX = 1f
        scaleY = 1f
    }

    private fun loop(
        target: View,
        property: String,
        durationMs: Long,
        delayMs: Long,
        vararg values: Float
    ) = ObjectAnimator.ofFloat(target, property, *values).apply {
        duration = durationMs
        startDelay = delayMs
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = DecelerateInterpolator()
    }

    private fun pulse(target: View, durationMs: Long, delayMs: Long) =
        ObjectAnimator.ofPropertyValuesHolder(
            target,
            PropertyValuesHolder.ofFloat("scaleX", 0.84f, 1.34f),
            PropertyValuesHolder.ofFloat("scaleY", 0.84f, 1.34f),
            PropertyValuesHolder.ofFloat("alpha", 0.52f, 0f)
        ).apply {
            duration = durationMs
            startDelay = delayMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = DecelerateInterpolator()
        }

    private fun dp(value: Int) = value * resources.displayMetrics.density
}
