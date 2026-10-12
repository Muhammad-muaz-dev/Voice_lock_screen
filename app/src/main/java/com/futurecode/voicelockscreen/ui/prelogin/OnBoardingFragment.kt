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
                imageRes = R.drawable.onboard_one,
                title = getString(R.string.onboarding_command_title),
                description = getString(R.string.onboarding_command_description)
            )
        )

        onboardingPages.add(
            OnBoardingPage(
                imageRes = R.drawable.onboard_two,
                title = getString(R.string.onboarding_recognition_title),
                description = getString(R.string.onboarding_recognition_description)
            )
        )

        onboardingPages.add(
            OnBoardingPage(
                imageRes = R.drawable.onboard_three,
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
        updateIndicators(0)
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
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(8, 0, 8, 0)
        }

        for (i in 0 until count) {
            val indicatorView = ImageView(requireContext())
            indicatorView.layoutParams = params
            binding.pageIndicator.addView(indicatorView)
        }
    }

    private fun updateIndicators(position: Int) {
        binding.btnNext.setText(if (position == onboardingPages.lastIndex) R.string.get_started else R.string.next)
        val count = binding.pageIndicator.childCount
        for (i in 0 until count) {
            val view = binding.pageIndicator.getChildAt(i) as ImageView
            if (i == position) {
                view.setImageResource(R.drawable.bg_indicator_active)
            } else {
                view.setImageResource(R.drawable.bg_indicator_inactive)
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
