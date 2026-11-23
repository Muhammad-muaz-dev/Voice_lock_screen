package com.futurecode.voicelockscreen.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.futurecode.voicelockscreen.ads.AdViews
import com.futurecode.voicelockscreen.ads.AdsConfig
import com.futurecode.voicelockscreen.ads.AdsManager
import com.futurecode.voicelockscreen.utils.FullBleed
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ActivityHomeBinding
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.utils.setLightStatusBarWithDarkIcons

class HomeActivity : BaseActivity() { // Assumed mapping, adjust if you inherit directly from AppCompatActivity

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    private var bannerRequested = false
    private var bannerLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIXED: Pehle static R.layout wala setContentView bilkul hata diya.
        // Ab hum sirf unified single-instantiated view binding instance content set karenge.
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val darkBackgroundColor = android.graphics.Color.parseColor("#0B1020")
        val lightBackgroundColor = getColor(R.color.bg)
        window.setLightStatusBarWithDarkIcons(lightBackgroundColor)

        // FIXED: Ab runtime binding safely initialized frame reference se fragment container fetch karega
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        ViewCompat.setOnApplyWindowInsetsListener(binding.sharedBannerSlot) { slot, insets ->
            slot.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
        val defaultNavigationBarColor = window.navigationBarColor
        val defaultLightNavigationBars = WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightNavigationBars
        val defaultNavigationContrast = Build.VERSION.SDK_INT >= 29 && window.isNavigationBarContrastEnforced
        var wasThemePreview = false
        var previousDestinationId: Int? = null
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val switchingTabs = previousDestinationId?.let(mainTabs::contains) == true &&
                destination.id in mainTabs
            previousDestinationId = destination.id
            val themePreview = destination.id == R.id.themePreviewFragment
            val darkPreview = destination.id == R.id.screenPreviewFragment ||
                destination.id == R.id.customThemeFragment
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (themePreview != wasThemePreview) {
                WindowCompat.setDecorFitsSystemWindows(window, !themePreview)
                window.navigationBarColor = if (themePreview) android.graphics.Color.TRANSPARENT else defaultNavigationBarColor
                insetsController.isAppearanceLightNavigationBars = !themePreview && defaultLightNavigationBars
                if (Build.VERSION.SDK_INT >= 29) {
                    window.isNavigationBarContrastEnforced = !themePreview && defaultNavigationContrast
                }
                wasThemePreview = themePreview
            }
            window.statusBarColor = when {
                themePreview -> android.graphics.Color.TRANSPARENT
                darkPreview -> darkBackgroundColor
                else -> lightBackgroundColor
            }
            insetsController.isAppearanceLightStatusBars = !darkPreview && !themePreview
            // Full-screen previews draw edge to edge; every other screen keeps the inset padding.
            FullBleed.set(this, darkPreview || themePreview || destination.id in mainTabs)
            updateSharedBanner()
            if (!switchingTabs) maybeShowCooldownAd(destination.id)
        }
        AdsManager.preloadCooldownInterstitial()
        if (intent.getBooleanExtra(EXTRA_SHOW_LOCK_PREVIEW, false)) {
            AppLockState.requestLockAfterPhoneUnlock(this)
            binding.root.post {
                showAppLockIfRequested()
            }
        }
        if (intent.getBooleanExtra(EXTRA_SHOW_PREMIUM, false)) {
            intent.removeExtra(EXTRA_SHOW_PREMIUM)
            binding.root.post {
                if (::navController.isInitialized) {
                    navController.navigate(R.id.premiumFragment)
                }
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_SHOW_LOCK_PREVIEW, false)) {
            AppLockState.requestLockAfterPhoneUnlock(this)
            binding.root.post {
                showAppLockIfRequested()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateSharedBanner()
        binding.root.post {
            showAppLockIfRequested()
        }
    }

    override fun onDestroy() {
        AdViews.clear(binding.adBannerContainer)
        super.onDestroy()
    }

    private fun updateSharedBanner() {
        val destinationId = navController.currentDestination?.id ?: return
        val eligible = destinationId in mainTabs && AdsConfig.homeBanner
        val visible = eligible && bannerLoaded
        if (binding.sharedBannerSlot.isVisible != visible) {
            binding.sharedBannerSlot.isVisible = visible
            ViewCompat.requestApplyInsets(binding.root)
        }
        if (!eligible || bannerRequested) return
        bannerRequested = true
        binding.root.doOnPreDraw {
            binding.root.post {
                if (isFinishing || isDestroyed) return@post
                AdViews.loadBanner(this, binding.adBannerContainer) { loaded ->
                    bannerLoaded = loaded
                    updateSharedBanner()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            AppLockState.setUnlockedThisSession(this, false)
        }
    }

    private fun showAppLockIfRequested() {
        val requestedByIntent = intent.getBooleanExtra(EXTRA_SHOW_LOCK_PREVIEW, false)
        val requestedByPhoneUnlock = AppLockState.consumePhoneUnlockRequest(this)
        intent.removeExtra(EXTRA_SHOW_LOCK_PREVIEW)
        if (!requestedByIntent && !requestedByPhoneUnlock) return

        showAppLockIfNeeded()
    }

    private fun showAppLockIfNeeded() {
        if (!::navController.isInitialized) return
        if (navController.currentDestination?.id == R.id.screenPreviewFragment) return
        if (!AppLockState.hasAnyLockEnabled(this)) return
        if (AppLockState.isUnlockedThisSession(this)) return

        navController.navigate(R.id.screenPreviewFragment)
    }

    private var firstDestinationSeen = false

    /**
     * Cooldown interstitial on every navigation after the first screen: opening a feature and
     * going back both change the destination. AdsManager enforces remote on/off and cooldown.
     */
    private fun maybeShowCooldownAd(destinationId: Int) {
        if (!firstDestinationSeen) {
            firstDestinationSeen = true
            return
        }
        if (destinationId in NO_COOLDOWN_AD) return
        AdsManager.showCooldownInterstitial(this)
    }

    companion object {
        private val mainTabs = setOf(R.id.homeFragment, R.id.themesFragment, R.id.voiceNoteFragment)

        const val EXTRA_SHOW_LOCK_PREVIEW = "extra_show_lock_preview"
        const val EXTRA_SHOW_PREMIUM = "extra_show_premium"

        /** Screens where a full-screen ad would interrupt a lock/unlock or purchase step. */
        private val NO_COOLDOWN_AD = setOf(
            R.id.successFragment,
            R.id.screenPreviewFragment,
            R.id.premiumFragment
        )
    }
}
