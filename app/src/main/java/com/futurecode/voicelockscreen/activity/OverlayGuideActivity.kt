package com.futurecode.voicelockscreen.activity

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ActivityOverlayGuideBinding

/**
 * Translucent guide launched right after the system overlay-permission page so the
 * user can see which entry to switch on. Dismissing it reveals the settings page.
 */
class OverlayGuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOverlayGuideBinding
    private val animators = mutableListOf<ValueAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOverlayGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindContent()
        setupActions()
    }

    override fun onStart() {
        super.onStart()
        startAnimations()
    }

    override fun onStop() {
        animators.forEach { it.cancel() }
        animators.clear()
        super.onStop()
    }

    private fun bindContent() {
        val appName = applicationInfo.loadLabel(packageManager).toString()
        binding.tvGuideAppName.text = appName
        binding.tvGuideDescription.text = getString(R.string.camera_enable_permission_desc, appName)
        binding.ivGuideAppIcon.setImageDrawable(applicationInfo.loadIcon(packageManager))
    }

    private fun setupActions() {
        binding.btnGuideGotIt.setOnClickListener { finish() }
        binding.guideRoot.setOnClickListener { finish() }
    }

    private fun startAnimations() {
        val density = resources.displayMetrics.density
        animators += loop(binding.ivGuideHand, View.TRANSLATION_Y, 0f, -6f * density)
        animators += loop(binding.ivGuideHand, View.TRANSLATION_X, 0f, -4f * density)
    }

    private fun loop(target: View, property: android.util.Property<View, Float>, from: Float, to: Float) =
        ObjectAnimator.ofFloat(target, property, from, to).apply {
            duration = TAP_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, android.R.anim.fade_out)
    }

    private companion object {
        const val TAP_MS = 600L
    }
}
