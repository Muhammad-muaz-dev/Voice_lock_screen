package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Intent
import android.graphics.Outline
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.BuildConfig
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSettingBinding
import com.futurecode.voicelockscreen.databinding.VnItemSettingBinding
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.ui.prelogin.LanguageFragment
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore

/** Settings, laid out like the reference: premium banner, "General settings" and "Other" cards. */
class SettingFragment : BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate) {

    private data class Row(@DrawableRes val icon: Int, @StringRes val title: Int, @StringRes val desc: Int, val action: () -> Unit)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VoiceNoteUi.lightStatusBar(this)
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        setupBanner()
        bindRows(
            binding.rcvGeneralSetting,
            listOf(
                Row(R.drawable.vn_ic_world, R.string.vn_text_change_language, R.string.vn_text_change_language_desc) {
                    findNavController().navigate(
                        R.id.languageFragment,
                        Bundle().apply { putBoolean(LanguageFragment.ARG_RETURN_TO_SETTINGS, true) }
                    )
                },
                Row(R.drawable.vn_ic_change_security, R.string.vn_text_change_security_question, R.string.vn_text_change_security_question_desc) {
                    findNavController().navigate(R.id.securityQuestionFragment, Bundle().apply { putBoolean("editing", true) })
                },
                Row(R.drawable.vn_ic_disable_lock, R.string.vn_text_disable_system_lock, R.string.vn_text_disable_system_lock_desc) {
                    VoiceLockSwitchStore.setUnlockEnabled(requireContext(), false)
                    LockService.stop(requireContext())
                    openIntent(Intent(Settings.ACTION_SECURITY_SETTINGS), getString(R.string.system_lock_settings_unavailable))
                }
            )
        )
        bindRows(
            binding.rcvOther,
            listOf(
                Row(R.drawable.vn_ic_share, R.string.vn_text_share_app, R.string.vn_text_share_app_desc) { shareApp() },
                Row(R.drawable.vn_ic_feedback, R.string.vn_text_feedback, R.string.vn_text_feedback_desc) { sendFeedback() },
                Row(R.drawable.vn_ic_privacy_policy, R.string.vn_text_policies, R.string.vn_text_policies_desc) { openPrivacyPolicy() },
                Row(R.drawable.vn_ic_rate, R.string.vn_text_rate_us, R.string.vn_text_rate_us_desc) {
                    VoiceNoteUi.showRate(requireContext(), onNotReally = { sendFeedback() }, onLoveIt = { openStore() })
                }
            )
        )
    }

    private fun setupBanner() {
        val radius = resources.getDimension(R.dimen.vn_dp_16)
        binding.frameBannerIAP.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
        binding.frameBannerIAP.clipToOutline = true
        if (prefManager.isPro) {
            binding.tvUpgradeTitle.setText(R.string.youre_premium)
            binding.tvPremiumDesc.setText(R.string.everything_unlocked)
            binding.btnUpgrade.setText(R.string.manage_plan)
        }
        val openPremium = View.OnClickListener {
            if (prefManager.isPro && !BuildConfig.DEBUG) {
                openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions")), getString(R.string.no_app_found))
            } else {
                findNavController().navigate(R.id.premiumFragment)
            }
        }
        binding.frameBannerIAP.setOnClickListener(openPremium)
        binding.btnUpgrade.setOnClickListener(openPremium)
    }

    private fun bindRows(list: RecyclerView, rows: List<Row>) {
        list.layoutManager = LinearLayoutManager(requireContext())
        list.isNestedScrollingEnabled = false
        list.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val row = VnItemSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return object : RecyclerView.ViewHolder(row.root) {}.also { it.itemView.tag = row }
            }

            override fun getItemCount() = rows.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val row = holder.itemView.tag as VnItemSettingBinding
                val item = rows[position]
                row.ivThumb.setImageResource(item.icon)
                row.tvTitle.setText(item.title)
                row.tvDesc.setText(item.desc)
                row.root.setOnClickListener { item.action() }
            }
        }
    }

    private fun shareApp() {
        val message = PrefManager.get(requireContext()).shareMessage.ifBlank {
            getString(R.string.share_app_message, getString(R.string.app_name), playStoreUrl())
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        runCatching { startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app))) }
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

    private fun openStore() = openIntent(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl())), getString(R.string.no_app_found))

    private fun openIntent(intent: Intent, errorMessage: String) {
        val context = context ?: return
        runCatching { startActivity(intent) }
            .onFailure { Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show() }
    }

    private fun playStoreUrl() = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
}
