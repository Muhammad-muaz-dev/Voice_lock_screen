package com.futurecode.voicelockscreen.ui.prelogin

import android.content.Intent
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.ads.AdsConfig

/**
 * Launch sequence: Splash -> In-app (Premium) -> Language -> Onboarding -> Home.
 * Each step can be switched off from Remote Config; Language and Onboarding run on first launch only.
 */
object PreloginFlow {

    enum class Step { SPLASH, INAPP, LANGUAGE, ONBOARDING }

    /** Argument telling PremiumFragment it is part of this flow (close = continue, not pop). */
    const val ARG_IN_FLOW = "prelogin_in_flow"

    private val order = listOf(Step.INAPP, Step.LANGUAGE, Step.ONBOARDING)

    /** Opens the first enabled step after [current], or Home when none is left. */
    fun continueFrom(fragment: Fragment, current: Step) {
        val remaining = order.dropWhile { current != Step.SPLASH && it.ordinal <= current.ordinal }
        val next = remaining.firstOrNull(::isEnabled)
        if (next == null) {
            openHome(fragment)
            return
        }
        val destination = when (next) {
            Step.INAPP -> R.id.premiumFragment
            Step.LANGUAGE -> R.id.languageFragment
            Step.ONBOARDING -> R.id.onboardingFragment
            Step.SPLASH -> return
        }
        val nav = fragment.findNavController()
        val currentId = nav.currentDestination?.id ?: return
        nav.navigate(
            destination,
            bundleOf(ARG_IN_FLOW to true),
            navOptions { popUpTo(currentId) { inclusive = true } }
        )
    }

    private fun isEnabled(step: Step): Boolean {
        val prefs = MyApplication.app.prefManager
        val firstRun = !prefs.isOnboardingDone
        return when (step) {
            Step.INAPP -> AdsConfig.inAppScreen && !prefs.isPro
            Step.LANGUAGE -> firstRun && AdsConfig.languageScreen
            Step.ONBOARDING -> firstRun && AdsConfig.onboardingScreen
            Step.SPLASH -> false
        }
    }

    /** True when the Language screen will be shown this launch (used to preload its native ad). */
    fun languageWillShow(): Boolean = isEnabled(Step.LANGUAGE)

    private fun openHome(fragment: Fragment) {
        val activity = fragment.requireActivity()
        if (activity.isFinishing) return
        activity.startActivity(Intent(activity, HomeActivity::class.java))
        MyApplication.app.prefManager.isOnboardingDone = true
        activity.finish()
    }
}
