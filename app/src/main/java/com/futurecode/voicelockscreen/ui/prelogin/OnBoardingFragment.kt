package com.futurecode.voicelockscreen.ui.prelogin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.adapter.OnBoardingAdapter
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentOnBoardingBinding
import com.futurecode.voicelockscreen.model.OnBoardingPage

class OnBoardingFragment : BaseFragment<FragmentOnBoardingBinding>(FragmentOnBoardingBinding::inflate) {

    private lateinit var adapter: OnBoardingAdapter
    private val onboardingPages = ArrayList<OnBoardingPage>()

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            updateIndicators(position)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData()
        setupViewPager()
        setupListeners()
    }

    private fun initData() {
        onboardingPages.clear()

        onboardingPages.add(
            OnBoardingPage(
                imageRes = R.drawable.onboarding_voice_midnight,
                title = getString(R.string.onboarding_command_title),
                description = getString(R.string.onboarding_command_description)
            )
        )

        onboardingPages.add(
            OnBoardingPage(
                imageRes = R.drawable.onboarding_camera_midnight,
                title = getString(R.string.onboarding_recognition_title),
                description = getString(R.string.onboarding_recognition_description)
            )
        )

        onboardingPages.add(
            OnBoardingPage(
                imageRes = R.drawable.onboarding_notes_midnight,
                title = getString(R.string.onboarding_protection_title),
                description = getString(R.string.onboarding_protection_description)
            )
        )
    }

    private fun setupViewPager() {
        adapter = OnBoardingAdapter(onboardingPages)
        binding.onBoardingPager.adapter = adapter
        binding.onBoardingPager.registerOnPageChangeCallback(pageChangeCallback)

        setupIndicators(onboardingPages.size)
        updateIndicators(binding.onBoardingPager.currentItem)
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            val currentPos = binding.onBoardingPager.currentItem
            if (currentPos < onboardingPages.size - 1) {
                binding.onBoardingPager.setCurrentItem(currentPos + 1, true)
            } else {
                goToHome()
            }
        }
    }

    private fun setupIndicators(count: Int) {
        binding.pageIndicator.removeAllViews()
        for (i in 0 until count) {
            val indicatorView = ImageView(requireContext())
            indicatorView.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.onboarding_dot_size),
                resources.getDimensionPixelSize(R.dimen.onboarding_dot_size)
            ).apply { marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_sm) }
            indicatorView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            binding.pageIndicator.addView(indicatorView)
        }
    }

    private fun updateIndicators(position: Int) {
        binding.btnNext.setText(R.string.next)
        binding.pageIndicator.contentDescription = getString(R.string.onboarding_page_position, position + 1, onboardingPages.size)
        val count = binding.pageIndicator.childCount
        for (i in 0 until count) {
            val view = binding.pageIndicator.getChildAt(i) as ImageView
            view.layoutParams = (view.layoutParams as LinearLayout.LayoutParams).apply {
                width = resources.getDimensionPixelSize(
                    if (i == position) R.dimen.onboarding_dot_active_width else R.dimen.onboarding_dot_size
                )
            }
            if (i == position) {
                view.setImageResource(R.drawable.bg_onboarding_dot_active)
            } else {
                view.setImageResource(R.drawable.bg_onboarding_dot_inactive)
            }
        }
    }

    private fun goToHome() {
        prefManager.isLanguageSelectedFirstTime = true
        prefManager.isOnboardingDone = true
        val intent = Intent(requireContext(), HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(HomeActivity.EXTRA_SHOW_PREMIUM, true)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        binding.onBoardingPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }
}
