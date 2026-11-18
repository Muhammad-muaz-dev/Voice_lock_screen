package com.futurecode.voicelockscreen.ui.prelogin

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSplashBinding
import com.futurecode.voicelockscreen.utils.JsonReadUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    private var progressAnimator: ValueAnimator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        JsonReadUtils.fetchJsonData(requireContext()) { }
        startProgress()
        navigation()
    }

    override fun onDestroyView() {
        progressAnimator?.cancel()
        progressAnimator = null
        super.onDestroyView()
    }

    private fun startProgress() {
        renderProgress(0f)
        progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SPLASH_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { renderProgress(it.animatedValue as Float) }
            start()
        }
    }

    private fun renderProgress(fraction: Float) {
        val binding = bindingOrNull ?: return
        binding.splashProgressBar.progress = fraction
        binding.tvSplashPercent.text = binding.splashProgressBar.percentText()
    }

    fun navigation() {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(SPLASH_DURATION_MS)
            if (prefManager.isOnboardingDone) {
                startActivity(Intent(requireContext(), HomeActivity::class.java))
                requireActivity().finish()
            } else {
                findNavController().navigate(R.id.action_splashFragment_to_languageFragment)
            }
        }
    }

    private companion object {
        const val SPLASH_DURATION_MS = 1200L
    }
}
