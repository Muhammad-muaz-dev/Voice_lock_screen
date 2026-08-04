package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.ads.interstitial_ad.FullScreenAdsHelper
import com.futurecode.voicelockscreen.ads.native_ad.NativeAdsHelper
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSettingBinding
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.ui.prelogin.LanguageFragment
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore

class SettingFragment : BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate) {

    private var nativeAdsHelper: NativeAdsHelper? = null
    private var fullScreenAdsHelper: FullScreenAdsHelper? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nativeAdsHelper = NativeAdsHelper(requireActivity())
        fullScreenAdsHelper = FullScreenAdsHelper(requireActivity())
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        loadNativeAds()
        binding.btnUpgrade.setOnClickListener {
            findNavController().navigate(R.id.premiumFragment)
        }

        binding.clChangeLanguage.setOnClickListener {
            findNavController().navigate(
                R.id.languageFragment,
                Bundle().apply {
                    putBoolean(LanguageFragment.ARG_RETURN_TO_SETTINGS, true)
                }
            )
        }

        binding.clSecurityQuestion.setOnClickListener {
            findNavController().navigate(R.id.securityQuestionFragment)
        }

        binding.clDisableLock.setOnClickListener {
            VoiceLockSwitchStore.setUnlockEnabled(requireContext(), false)
            LockService.stop(requireContext())
            openSystemSecuritySettings()
        }

        binding.clShareApp.setOnClickListener {
            shareApp()
        }

        binding.clFeedback.setOnClickListener {
            sendFeedback()
        }

        binding.clPrivacy.setOnClickListener {
            openPrivacyPolicy()
        }
    }

    private fun openSystemSecuritySettings() {
        openIntent(
            Intent(Settings.ACTION_SECURITY_SETTINGS),
            getString(R.string.system_lock_settings_unavailable)
        )
    }

    private fun shareApp() {
        val playStoreUrl = playStoreUrl()
        val configuredMessage = PrefManager.get(requireContext()).shareMessage
        val message = configuredMessage.ifBlank {
            getString(R.string.share_app_message, getString(R.string.app_name), playStoreUrl)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)))
    }

    private fun sendFeedback() {
        val contact = PrefManager.get(requireContext()).contactUs.trim()
        val intent = if (contact.startsWith("http://") || contact.startsWith("https://")) {
            Intent(Intent.ACTION_VIEW, Uri.parse(contact))
        } else {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${contact.takeIf { it.contains("@") }.orEmpty()}")
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_email_subject, getString(R.string.app_name)))
            }
        }
        openIntent(intent, getString(R.string.no_app_found))
    }

    private fun openPrivacyPolicy() {
        val privacyUrl = PrefManager.get(requireContext()).privacyPolicy.trim()
        if (privacyUrl.isBlank()) {
            Toast.makeText(requireContext(), R.string.privacy_policy_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        openIntent(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)), getString(R.string.no_app_found))
    }

    private fun openIntent(intent: Intent, errorMessage: String) {
        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
    }

    private fun playStoreUrl(): String {
        return "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
    }

    fun loadNativeAds() {
        activity?.let { currentActivity ->
            if (nativeAdsHelper == null) {
                nativeAdsHelper = NativeAdsHelper(currentActivity)
            }
            nativeAdsHelper?.showNativeAd(
                nativeBannerAdView = binding.nativeAds3.frame,
                mainLayout = binding.nativeAds3.mainLayout,
                placeholder = binding.nativeAds3.placeholder
            )
        }
    }

}
