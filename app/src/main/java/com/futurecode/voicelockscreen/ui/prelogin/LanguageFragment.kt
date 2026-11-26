package com.futurecode.voicelockscreen.ui.prelogin

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.adapter.LanguageAdapter
import com.futurecode.voicelockscreen.ads.AdPlacement
import com.futurecode.voicelockscreen.ads.AdViews
import com.futurecode.voicelockscreen.ads.AdsConfig
import com.futurecode.voicelockscreen.ads.AdsManager
import com.futurecode.voicelockscreen.ads.ScreenAdConfig
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentLanguageBinding
import com.futurecode.voicelockscreen.model.LanguageOption

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {
    private lateinit var languageAdapter: LanguageAdapter
    private var selectedLanguageCode: String? = null
    private var englishHintDismissed = false
    private var englishPosition = RecyclerView.NO_POSITION
    private var handAnimators = emptyList<ObjectAnimator>()
    private val languageScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            updateEnglishHintPosition()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        englishHintDismissed = savedInstanceState?.getBoolean(STATE_ENGLISH_HINT_DISMISSED) ?: false
        val languages = languageOptions()
        englishPosition = languages.indexOfFirst { it.code == ENGLISH_LANGUAGE_CODE }
        val stored = savedInstanceState?.getString(STATE_SELECTION)
            ?: MyApplication.app.prefManager.selectedLanguage
        val supportedCodes = languages.map { it.code }.toSet()
        selectedLanguageCode = stored.substringBefore('-').let { if (it == "in") "id" else it }
            .takeIf { it.isNotBlank() && it in supportedCodes }

        languageAdapter = LanguageAdapter(languages, selectedLanguageCode) { language ->
            englishHintDismissed = true
            hideEnglishHint()
            selectedLanguageCode = language.code
            languageAdapter.updateSelection(language.code)
        }
        binding.rvLanguages.adapter = languageAdapter
        binding.rvLanguages.addOnScrollListener(languageScrollListener)
        binding.btnContinue.setOnClickListener { confirmLanguage() }
        showNativeAd()
        binding.rvLanguages.post { updateEnglishHintPosition() }
    }

    override fun onResume() {
        super.onResume()
        if (::languageAdapter.isInitialized) binding.rvLanguages.post { updateEnglishHintPosition() }
    }

    override fun onPause() {
        hideEnglishHint()
        super.onPause()
    }

    override fun onDestroyView() {
        bindingOrNull?.rvLanguages?.removeOnScrollListener(languageScrollListener)
        hideEnglishHint()
        super.onDestroyView()
    }

    /** Native at the bottom, only in the launch flow (not when opened from Settings). */
    private fun showNativeAd() {
        AdViews.clearOnDetach(binding.languageAdContainer)
        if (isFromSettings()) return
        val config = AdsConfig.languageAds
        if (!AdsConfig.adsAllowed) return
        if (config.native) {
            val nativeLayout = R.layout.layout_native_ad_onboarding
            val nativeConfig = config.copy(nativeFormat = ScreenAdConfig.FORMAT_MEDIA)
            // The native was started during splash; this hands it over (or waits for it).
            AdViews.showNativeShimmer(binding.languageAdContainer, nativeConfig, nativeLayout)
            val requestToken = Any()
            binding.languageAdContainer.tag = requestToken
            AdsManager.takeLanguageNative { ad ->
                val b = bindingOrNull
                when {
                    b == null -> ad?.destroy()
                    b.languageAdContainer.tag !== requestToken -> ad?.destroy()
                    ad == null -> AdViews.clear(b.languageAdContainer)
                    else -> AdViews.showNative(b.languageAdContainer, ad, nativeConfig, nativeLayout)
                }
            }
        } else {
            AdPlacement.show(this, binding.languageAdContainer, config)
        }
    }

    private fun isFromSettings() = arguments?.getBoolean(ARG_RETURN_TO_SETTINGS, false) == true

    private fun updateEnglishHintPosition() {
        val currentBinding = bindingOrNull ?: return
        if (!isResumed || englishHintDismissed || englishPosition == RecyclerView.NO_POSITION) {
            hideEnglishHint()
            return
        }

        val englishRow = currentBinding.rvLanguages
            .findViewHolderForAdapterPosition(englishPosition)
            ?.itemView
        if (englishRow == null) {
            hideEnglishHint()
            return
        }

        val hand = currentBinding.ivEnglishHint
        val rowLocation = IntArray(2)
        val overlayLocation = IntArray(2)
        englishRow.getLocationOnScreen(rowLocation)
        currentBinding.languageListOverlay.getLocationOnScreen(overlayLocation)

        val layoutParams = hand.layoutParams as FrameLayout.LayoutParams
        val handHeight = hand.height.takeIf { it > 0 } ?: layoutParams.height
        layoutParams.topMargin = (
            rowLocation[1] - overlayLocation[1] + (englishRow.height - handHeight) / 2
            ).coerceAtLeast(0)
        hand.layoutParams = layoutParams
        hand.scaleX = if (hand.layoutDirection == View.LAYOUT_DIRECTION_RTL) -1f else 1f
        hand.isVisible = true
        if (handAnimators.isEmpty()) startEnglishHintAnimation(hand)
    }

    private fun startEnglishHintAnimation(hand: View) {
        val density = resources.displayMetrics.density
        val horizontalTarget = if (hand.layoutDirection == View.LAYOUT_DIRECTION_RTL) 4f else -4f
        handAnimators = listOf(
            loop(hand, View.TRANSLATION_Y, 0f, -6f * density),
            loop(hand, View.TRANSLATION_X, 0f, horizontalTarget * density)
        )
        handAnimators.forEach(ObjectAnimator::start)
    }

    private fun hideEnglishHint() {
        handAnimators.forEach(ObjectAnimator::cancel)
        handAnimators = emptyList()
        bindingOrNull?.ivEnglishHint?.let { hand ->
            hand.isVisible = false
            hand.translationX = 0f
            hand.translationY = 0f
            hand.scaleX = 1f
        }
    }

    private fun loop(target: View, property: android.util.Property<View, Float>, from: Float, to: Float) =
        ObjectAnimator.ofFloat(target, property, from, to).apply {
            duration = HINT_ANIMATION_DURATION_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }

    private fun confirmLanguage() {
        val languageCode = selectedLanguageCode
            ?.takeUnless { it == SYSTEM_DEFAULT_LANGUAGE_CODE }
            .orEmpty()
        if (isFromSettings()) {
            findNavController().popBackStack()
        } else {
            // Navigate before applying the locale: the locale change recreates the activity and the
            // restored back stack keeps the next screen.
            PreloginFlow.continueFrom(this, PreloginFlow.Step.LANGUAGE)
        }
        MyApplication.applyLanguage(languageCode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTION, selectedLanguageCode)
        outState.putBoolean(STATE_ENGLISH_HINT_DISMISSED, englishHintDismissed)
    }

    private fun languageOptions(): List<LanguageOption> {
        fun option(code: String, label: String, country: String) =
            LanguageOption(code, label, label, country)

        return listOf(
            option(SYSTEM_DEFAULT_LANGUAGE_CODE, getString(R.string.language_system_default), ""),
            option("hi", "हिन्दी", "IN"),
            option(ENGLISH_LANGUAGE_CODE, "English", "GB"),
            option("de", "Deutsch", "DE"),
            option("it", "Italiano", "IT"),
            option("ar", "العربية", "SA"),
            option("ja", "日本語", "JP"),
            option("ko", "한국어", "KR"),
            option("es", "Español", "ES"),
            option("id", "Bahasa Indonesia", "ID"),
            option("pt", "Português", "PT"),
            option("fr", "Français", "FR"),
            option("tr", "Türkçe", "TR"),
            option("ms", "Bahasa Melayu", "MY")
        )
    }

    companion object {
        const val ARG_RETURN_TO_SETTINGS = "return_to_settings"
        private const val SYSTEM_DEFAULT_LANGUAGE_CODE = "system_default"
        private const val ENGLISH_LANGUAGE_CODE = "en"
        private const val STATE_SELECTION = "language_selection"
        private const val STATE_ENGLISH_HINT_DISMISSED = "english_hint_dismissed"
        private const val HINT_ANIMATION_DURATION_MS = 600L
    }
}
