package com.futurecode.voicelockscreen.ui.prelogin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
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
import com.futurecode.voicelockscreen.ads.interstitial_ad.FullScreenAdsHelper
import com.futurecode.voicelockscreen.ads.native_ad.NativeAdsHelper
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentOnBoardingBinding
import com.futurecode.voicelockscreen.model.OnBoardingPage
import com.futurecode.voicelockscreen.utils.Utils.setAdClickListener
import kotlin.text.clear
class OnBoardingFragment : BaseFragment<FragmentOnBoardingBinding>(FragmentOnBoardingBinding::inflate) {

    private lateinit var adapter: OnBoardingAdapter
    private val onboardingPages = ArrayList<OnBoardingPage>()

    // ✅ Added dynamic timers controllers for ad synchronization
    private var timerController: AdPagerTimerController? = null

    private lateinit var nativeAdsHelper: NativeAdsHelper
    lateinit var fullScreenAdsHelper: FullScreenAdsHelper

    // VIEW PAGER RE-ROUTING LAMBDA TO AVOID UN-SYNCED STATE CRASHES
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if (::adapter.isInitialized) {
                adapter.onPageSelected(position)
                updateUIForPosition(position)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nativeAdsHelper = NativeAdsHelper(requireActivity())
        fullScreenAdsHelper = FullScreenAdsHelper(requireActivity())

        initData()
        setupViewPager()
        setupListeners()
    }

    private fun initData() {
        onboardingPages.clear()

        // Page 1 Setup
        onboardingPages.add(
            OnBoardingPage(
                imageRes = R.drawable.onboard_one,
                title = getString(R.string.onboarding_command_title),
                description = getString(R.string.onboarding_command_description)
            )
        )

        // Page 2 Setup
        onboardingPages.add(
            OnBoardingPage(
                imageRes = R.drawable.onboard_two,
                title = getString(R.string.onboarding_recognition_title),
                description = getString(R.string.onboarding_recognition_description)
            )
        )

        // Page 3 Setup
        onboardingPages.add(
            OnBoardingPage(
                imageRes = R.drawable.onboard_three,
                title = getString(R.string.onboarding_protection_title),
                description = getString(R.string.onboarding_protection_description)
            )
        )
    }

    private fun setupViewPager() {
        // 1. Build Ad placement mapping patterns configuration
        var isAdsEnabled = !MyApplication.app.prefManager.adsOff

        val adConfig = AdPagerPlacementConfig(
            adsEnabled = isAdsEnabled,
            timerAdPagerPositions = setOf(1, 3), // Positions where native ads inject automatically
            timerUnlockDurationMs = 3_000L
        )

        // 2. Map pure model pages array list to generic AdPager items
        val pagerItems = AdEnabledPagerMapper.build(onboardingPages, adConfig)
        val nativeAdPagerController = NativeAdPagerController(ExistingNativeAdPageLoader(requireActivity()))

        val pagerTimerController = AdPagerTimerController()
        timerController = pagerTimerController

        // 3. Initialize compatible updated adapter
        adapter = OnBoardingAdapter(
            activity = requireActivity(),
            list = pagerItems,
            nativeAdPagerController = nativeAdPagerController,
            timerController = pagerTimerController,
            onAdAdvanceRequested = { position ->
                adapter.nextPositionAfter(position)?.let { nextPosition ->
                    binding.onBoardingPager.setCurrentItem(nextPosition, true)
                }


                // ✅ SAFE CHECK: Ensures View is active before modifying ViewPager
                if (isAdded && bindingOrNull != null) {
                    adapter.nextPositionAfter(position)?.let { nextPosition ->
                        bindingOrNull?.onBoardingPager?.setCurrentItem(nextPosition, true)
                    }
                }
            },
            onContentContinueRequested = { position ->
                handleContinue(position)
            }
        )

        binding.onBoardingPager.adapter = adapter
        binding.onBoardingPager.offscreenPageLimit = 3
        binding.onBoardingPager.registerOnPageChangeCallback(pageChangeCallback)

        // Generate indicators based strictly on raw content image items
        setupIndicators(onboardingPages.size)

        binding.onBoardingPager.post {
            if (isAdded && ::adapter.isInitialized) {
                val currentPos = binding.onBoardingPager.currentItem
                adapter.onPageSelected(currentPos)
                updateUIForPosition(currentPos)
            }
        }
    }

    private fun setupListeners() {

        binding.btnNext.setOnClickListener {
            MyApplication.app.prefManager.isOnboardingDone = true
            val intent = Intent(requireContext(), HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

    }

    private fun handleContinue(position: Int) {
        if (adapter.isLastContentPage(position)) {
            completeOnboarding()
        } else {
            adapter.nextPositionAfter(position)?.let { nextPosition ->
                binding.onBoardingPager.setCurrentItem(nextPosition, true)
            }
        }
    }

    // Controls indicators rendering visibility states on ad interruptions
    private fun updateUIForPosition(position: Int) {
        // ✅ SAFE CHECK: Use bindingOrNull to avoid IllegalStateException
        val safeBinding = bindingOrNull ?: return
        if (!isAdded) return

        val item = adapter.list.getOrNull(position)

        if (item is AdPagerItem.Content) {
            safeBinding.pageIndicator.visibility = View.VISIBLE
            safeBinding.btnNext.visibility = View.VISIBLE
           // safeBinding.btnSkip.visibility = View.VISIBLE
            updateIndicators(position)
        } else {
            safeBinding.pageIndicator.visibility = View.GONE
            safeBinding.btnNext.visibility = View.GONE
           // safeBinding.btnSkip.visibility = View.GONE
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
        val count = binding.pageIndicator.childCount

        // Maps global ViewPager item index safely into local content pages references array mapping
        val contentIndices = adapter.list.indices.filter { adapter.list[it] is AdPagerItem.Content }
        val visualActiveIndex = contentIndices.indexOf(position)

        for (i in 0 until count) {
            val view = binding.pageIndicator.getChildAt(i) as ImageView
            if (i == visualActiveIndex) {
                view.setImageResource(R.drawable.bg_indicator_active)
            } else {
                view.setImageResource(R.drawable.bg_indicator_inactive)
            }
        }
    }

    private fun completeOnboarding() {
        // ✅ SAFE CHECK: Verifies viewLifecycle and destination before navigating
        if (!isAdded || view == null) return

        prefManager.isLanguageSelectedFirstTime = true
        prefManager.isOnboardingDone = true

        try {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.onBoardingFragment) {
               // navController.navigate(R.id.action_onBoardingFragment_to_questionsFragment)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onDestroyView() {
        // Unregister callback safely before nullifying binding
        binding.onBoardingPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        if (::adapter.isInitialized) {
            adapter.release()
        }
        timerController?.clear()
        timerController = null
        super.onDestroyView()
    }
}
