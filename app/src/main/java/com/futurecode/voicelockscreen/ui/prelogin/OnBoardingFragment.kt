package com.futurecode.voicelockscreen.ui.prelogin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.adapter.OnBoardingAdapter
import com.futurecode.voicelockscreen.ads.adpager.AdEnabledPagerMapper
import com.futurecode.voicelockscreen.ads.adpager.AdPagerAdType
import com.futurecode.voicelockscreen.ads.adpager.AdPagerItem
import com.futurecode.voicelockscreen.ads.adpager.AdPagerPlacementConfig
import com.futurecode.voicelockscreen.ads.adpager.AdPagerTimerController
import com.futurecode.voicelockscreen.ads.ads_new.ExistingNativeAdPageLoader
import com.futurecode.voicelockscreen.ads.ads_new.NativeAdPagerController
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentOnBoardingBinding
import com.futurecode.voicelockscreen.model.OnBoardingPage

class OnBoardingFragment : BaseFragment<FragmentOnBoardingBinding>(FragmentOnBoardingBinding::inflate) {

    private val pages = listOf(
        OnBoardingPage(
            imageRes = R.drawable.onboard_one,
            title = "Command With\nYour Voice",
            description = "Unlock your phone using\nadvanced AI Voice recognition"
        ),
        OnBoardingPage(
            imageRes = R.drawable.onboard_two,
            title = "Smart Voice\nRecognition",
            description = "Your unique voice keeps\nyour phone secure"
        ),
        OnBoardingPage(
            imageRes = R.drawable.onboard_three,
            title = "All-In-One\nProtection",
            description = "Multiple Lock Options for Stronger\nEveryday Phone Security"
        )
    )

    private lateinit var onboardingAdapter: OnBoardingAdapter
    private var mixedPagerItems = listOf<AdPagerItem<OnBoardingPage>>()

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (::onboardingAdapter.isInitialized) {
                onboardingAdapter.onPageSelected(position)
                updateIndicator(contentIndexFor(position))
                updateNextButton(position)
                updateControlsFor(position)
            }
        }
    }

    private val nativeAdPagerController by lazy {
        NativeAdPagerController(ExistingNativeAdPageLoader(requireActivity()))
    }

    private val timerController by lazy {
        AdPagerTimerController()
    }

    private val initialPagerSyncRunnable = Runnable {
        val currentBinding = bindingOrNull ?: return@Runnable
        if (::onboardingAdapter.isInitialized && view != null) {
            val currentPos = currentBinding.onBoardingPager.currentItem
            onboardingAdapter.onPageSelected(currentPos)
            updateIndicator(contentIndexFor(currentPos))
            updateNextButton(currentPos)
            updateControlsFor(currentPos)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPager()
        binding.onBoardingPager.registerOnPageChangeCallback(pageChangeCallback)
        setupIndicator()
        updateIndicator(0)
        updateNextButton(0)
        updateControlsFor(0)

        binding.btnNext.setOnClickListener {
            val currentPage = binding.onBoardingPager.currentItem
            if (::onboardingAdapter.isInitialized && onboardingAdapter.isLastContentPage(currentPage)) {
                finishOnBoarding()
            } else {
                goToNextPagerPosition(currentPage)
            }
        }
    }

    override fun onDestroyView() {
        bindingOrNull?.onBoardingPager?.removeCallbacks(initialPagerSyncRunnable)
        bindingOrNull?.onBoardingPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        if (::onboardingAdapter.isInitialized) {
            onboardingAdapter.release()
        }
        super.onDestroyView()
    }

    private fun setupPager() {
        mixedPagerItems = AdEnabledPagerMapper.build(
            contentItems = pages,
            config = AdPagerPlacementConfig(
                adsEnabled = !MyApplication.app.prefManager.adsOff,
                adAfterEveryContentItems = 1,
                intervalAdType = AdPagerAdType.NORMAL
            )
        )

        onboardingAdapter = OnBoardingAdapter(
            activity = requireActivity(),
            list = mixedPagerItems,
            nativeAdPagerController = nativeAdPagerController,
            timerController = timerController,
            onAdAdvanceRequested = ::goToNextPagerPosition,
            onContentContinueRequested = ::goToNextPagerPosition
        )

        binding.onBoardingPager.adapter = onboardingAdapter
        binding.onBoardingPager.offscreenPageLimit = 1
        binding.onBoardingPager.post(initialPagerSyncRunnable)
    }

    private fun goToNextPagerPosition(position: Int) {
        val nextPosition = onboardingAdapter.nextPositionAfter(position)
        if (nextPosition == null) {
            finishOnBoarding()
        } else {
            binding.onBoardingPager.currentItem = nextPosition
        }
    }

    private fun setupIndicator() {
        binding.pageIndicator.removeAllViews()
        pages.forEach { _ ->
            val indicator = View(requireContext())
            binding.pageIndicator.addView(indicator)
        }
    }

    private fun updateIndicator(selectedPosition: Int) {
        for (index in 0 until binding.pageIndicator.childCount) {
            val indicator = binding.pageIndicator.getChildAt(index)
            val isSelected = index == selectedPosition
            val width = if (isSelected) 42.dp else 14.dp
            val height = 14.dp
            val margin = 5.dp

            indicator.background = ContextCompat.getDrawable(
                requireContext(),
                if (isSelected) R.drawable.bg_indicator_active else R.drawable.bg_indicator_inactive
            )
            indicator.layoutParams = LinearLayout.LayoutParams(width, height).apply {
                marginStart = margin
                marginEnd = margin
            }
        }
    }

    private fun updateNextButton(position: Int) {
        binding.btnNext.text = getString(
            if (onboardingAdapter.isLastContentPage(position)) R.string.get_started else R.string.next
        )
    }

    private fun updateControlsFor(position: Int) {
        val isAdPage = mixedPagerItems.getOrNull(position) is AdPagerItem.NativeAd
        binding.pageIndicator.visibility = if (isAdPage) View.GONE else View.VISIBLE
        binding.btnNext.visibility = if (isAdPage) View.GONE else View.VISIBLE
    }

    private fun contentIndexFor(position: Int): Int {
        val currentItem = mixedPagerItems.getOrNull(position)
        if (currentItem is AdPagerItem.Content) return currentItem.contentIndex

        return mixedPagerItems
            .take(position + 1)
            .lastOrNull { it is AdPagerItem.Content }
            ?.let { (it as AdPagerItem.Content).contentIndex }
            ?: 0
    }

    private fun finishOnBoarding() {
        MyApplication.app.prefManager.isOnboardingDone = true
        val intent = Intent(requireContext(), HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

}
