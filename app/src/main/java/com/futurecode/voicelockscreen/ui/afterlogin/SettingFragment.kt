package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSettingBinding
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.ui.prelogin.LanguageFragment
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore

class SettingFragment : BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        if (prefManager.isPro) {
            binding.tvUpgradeTitle.setText(R.string.youre_premium)
            binding.tvPremiumDesc.setText(R.string.everything_unlocked)
            binding.btnUpgrade.setText(R.string.manage_plan)
        }
        binding.btnUpgrade.setOnClickListener {
            if (prefManager.isPro && !com.futurecode.voicelockscreen.BuildConfig.DEBUG) {
                openIntent(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/account/subscriptions")),getString(R.string.no_app_found))
            } else findNavController().navigate(R.id.premiumFragment)
        }



        binding.clChangeLanguage.setOnClickListener {
            findNavController().navigate(
                R.id.languageFragment,
                Bundle().apply {
                    putBoolean(LanguageFragment.ARG_RETURN_TO_SETTINGS, true)
                }
            )
        }

//        binding.clSecurityQuestion.setOnClickListener {
//            findNavController().navigate(R.id.securityQuestionFragment)
//        }

        binding.clSecurityQuestion.setOnClickListener {
            findNavController().navigate(R.id.securityQuestionFragment, Bundle().apply { putBoolean("editing", true) })
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
        binding.btnRate.setOnClickListener {
            openIntent(Intent(Intent.ACTION_VIEW,Uri.parse(playStoreUrl())),getString(R.string.no_app_found))
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

}
