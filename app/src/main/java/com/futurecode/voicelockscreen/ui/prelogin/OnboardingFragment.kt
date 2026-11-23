package com.futurecode.voicelockscreen.ui.prelogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.ads.AdPlacement
import com.futurecode.voicelockscreen.ads.AdViews
import com.futurecode.voicelockscreen.ads.AdsConfig
import com.futurecode.voicelockscreen.ads.AdsManager
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentOnboardingBinding
import com.futurecode.voicelockscreen.databinding.ItemOnboardingPageBinding

/** First-launch onboarding: four branded pages, then Home. */
class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(FragmentOnboardingBinding::inflate) {

    /** A floating feature badge: icon, background, and position over the phone (bias 0..1). */
    private data class Badge(
        @DrawableRes val icon: Int,
        @DrawableRes val background: Int,
        val horizontalBias: Float,
        val verticalBias: Float
    )

    private data class Page(
        @DrawableRes val mockup: Int,
        @StringRes val title: Int,
        @StringRes val body: Int,
        val badges: List<Badge> = emptyList(),
        val showShield: Boolean = false
    )

    private val allPages = listOf(
        Page(R.drawable.img_onboard_1, R.string.onboard_title_1, R.string.onboard_body_1),
        Page(
            R.drawable.img_onboard_2, R.string.onboard_title_2, R.string.onboard_body_2,
            listOf(Badge(R.drawable.ic_badge_theme, R.drawable.bg_badge_orange, 1f, 0.14f))
        ),
        Page(
            R.drawable.img_onboard_3, R.string.onboard_title_3, R.string.onboard_body_3,
            listOf(
                Badge(R.drawable.vn_ic_mic_white, R.drawable.bg_badge_orange, 0f, 0.1f),
                Badge(R.drawable.ic_badge_pin, R.drawable.bg_badge_blue, 1f, 0.42f),
                Badge(R.drawable.ic_badge_pattern, R.drawable.bg_badge_purple, 0f, 0.66f),
                Badge(R.drawable.ic_badge_fingerprint, R.drawable.bg_badge_pink, 1f, 0.9f)
            )
        ),
        Page(
            R.drawable.img_onboard_4, R.string.onboard_title_4, R.string.onboard_body_4,
            listOf(
                Badge(R.drawable.ic_badge_fingerprint, R.drawable.bg_badge_blue, 0f, 0.08f),
                Badge(R.drawable.ic_badge_intruder, R.drawable.bg_badge_pink, 1f, 0.84f)
            ),
            showShield = true
        )
    )

    /** Pages switched on from remote config (falls back to all pages if every page is off). */
    private val pages by lazy {
        val enabled = AdsConfig.enabledOnboardingPages().mapNotNull { allPages.getOrNull(it - 1) }
        enabled.ifEmpty { allPages }
    }

    /** Pages that already got their one ad refresh. */
    private val refreshedPages = mutableSetOf<Int>()

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            renderPosition(position)
            refreshAdFor(position)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vpOnboarding.adapter = PageAdapter()
        binding.vpOnboarding.registerOnPageChangeCallback(pageCallback)
        binding.btnOnboardNext.setOnClickListener { onNext() }
        binding.btnOnboardSkip.setOnClickListener { finishOnboarding() }
        AdViews.clearOnDetach(binding.onboardAdContainer)
        renderPosition(binding.vpOnboarding.currentItem)
        refreshAdFor(binding.vpOnboarding.currentItem)
    }

    override fun onDestroyView() {
        binding.vpOnboarding.unregisterOnPageChangeCallback(pageCallback)
        super.onDestroyView()
    }

    private fun onNext() {
        val next = binding.vpOnboarding.currentItem + 1
        if (next < pages.size) binding.vpOnboarding.currentItem = next else finishOnboarding()
    }

    private fun finishOnboarding() {
        PreloginFlow.continueFrom(this, PreloginFlow.Step.ONBOARDING)
    }

    /**
     * Loads a fresh native the first time each page is shown. The ad area (35% of the screen)
     * appears only once an ad has loaded; until then the onboarding uses the full height.
     */
    private fun refreshAdFor(position: Int) {
        val config = AdsConfig.onboardingAds
        if (!AdsConfig.adsAllowed || !refreshedPages.add(position)) return
        if (config.native) {
            setAdAreaHeight(nativeArea = true)
            AdPlacement.show(this, binding.onboardAdContainer, config)
        } else if (config.banner && position == 0) {
            // A banner refreshes by itself; load it once.
            setAdAreaHeight(nativeArea = false)
            AdPlacement.show(this, binding.onboardAdContainer, config)
        }
    }

    private fun renderPosition(position: Int) {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        dots.forEachIndexed { index, dot -> dot.isVisible = index < pages.size }
        dots.forEachIndexed { index, dot ->
            val active = index == position
            dot.setBackgroundResource(if (active) R.drawable.bg_onboard_dot_active else R.drawable.bg_onboard_dot)
            dot.layoutParams = dot.layoutParams.apply { width = dp(if (active) ACTIVE_DOT_DP else DOT_DP) }
        }
        binding.btnOnboardNext.setText(if (position == pages.lastIndex) R.string.onboard_start else R.string.next)
        val page = pages[position]
        binding.tvOnboardTitle.setText(page.title)
        binding.tvOnboardBody.setText(page.body)
        listOf(binding.tvOnboardTitle, binding.tvOnboardBody).forEach {
            it.alpha = 0f
            it.animate().alpha(1f).setDuration(TEXT_FADE_MS).start()
        }
    }

    /** Native: the ad takes 35% of the height (content 65%). Banner: the ad wraps its own height. */
    private fun setAdAreaHeight(nativeArea: Boolean) {
        val params = binding.onboardAdContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        if (nativeArea) {
            params.height = 0
            params.matchConstraintPercentHeight = NATIVE_AREA_PERCENT
        } else {
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            params.matchConstraintPercentHeight = 1f
        }
        binding.onboardAdContainer.layoutParams = params
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private inner class PageAdapter : RecyclerView.Adapter<PageAdapter.Holder>() {

        inner class Holder(val page: ItemOnboardingPageBinding) : RecyclerView.ViewHolder(page.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = pages.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val page = pages[position]
            val view = holder.page
            bindMockup(view.ivMockup, page.mockup)
            view.ivShield.isVisible = page.showShield
            val slots = listOf(
                view.badgeOne to view.badgeOneIcon,
                view.badgeTwo to view.badgeTwoIcon,
                view.badgeThree to view.badgeThreeIcon,
                view.badgeFour to view.badgeFourIcon
            )
            slots.forEachIndexed { index, (frame, icon) -> bindBadge(frame, icon, page.badges.getOrNull(index)) }
        }

        private fun bindMockup(mockup: ImageView, @DrawableRes artwork: Int) {
            val isVoiceArtwork = artwork == R.drawable.img_onboard_1
            val gutter = resources.getDimensionPixelSize(
                if (isVoiceArtwork) R.dimen.spacing_lg else R.dimen.spacing_xxxl
            )
            mockup.layoutParams = (mockup.layoutParams as ConstraintLayout.LayoutParams).apply {
                marginStart = gutter
                marginEnd = gutter
                dimensionRatio = if (isVoiceArtwork) VOICE_ARTWORK_RATIO else null
            }
            mockup.scaleType = if (isVoiceArtwork) ImageView.ScaleType.FIT_XY else ImageView.ScaleType.FIT_CENTER
            mockup.setImageResource(artwork)
        }

        private fun bindBadge(frame: FrameLayout, icon: ImageView, badge: Badge?) {
            frame.isVisible = badge != null
            if (badge == null) return
            frame.setBackgroundResource(badge.background)
            icon.setImageResource(badge.icon)
            frame.layoutParams = (frame.layoutParams as ConstraintLayout.LayoutParams).apply {
                horizontalBias = badge.horizontalBias
                verticalBias = badge.verticalBias
            }
        }
    }

    private companion object {
        // Slightly widen the full artwork, including its transparent shadow clearance.
        const val VOICE_ARTWORK_RATIO = "0.56:1"
        const val DOT_DP = 8
        const val ACTIVE_DOT_DP = 28
        const val TEXT_FADE_MS = 220L
        const val NATIVE_AREA_PERCENT = 0.35f
    }
}
