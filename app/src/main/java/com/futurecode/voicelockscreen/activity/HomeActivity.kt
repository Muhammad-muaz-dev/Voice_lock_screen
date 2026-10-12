package com.futurecode.voicelockscreen.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ActivityHomeBinding
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.utils.setDarkStatusBarWithWhiteIcons

class HomeActivity : BaseActivity() { // Assumed mapping, adjust if you inherit directly from AppCompatActivity

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIXED: Pehle static R.layout wala setContentView bilkul hata diya.
        // Ab hum sirf unified single-instantiated view binding instance content set karenge.
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val darkBackgroundColor = Color.parseColor("#0B1020")
        window.setDarkStatusBarWithWhiteIcons(darkBackgroundColor)

        // FIXED: Ab runtime binding safely initialized frame reference se fragment container fetch karega
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
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
        binding.root.post {
            showAppLockIfRequested()
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

    companion object {
        const val EXTRA_SHOW_LOCK_PREVIEW = "extra_show_lock_preview"
        const val EXTRA_SHOW_PREMIUM = "extra_show_premium"
    }
}
