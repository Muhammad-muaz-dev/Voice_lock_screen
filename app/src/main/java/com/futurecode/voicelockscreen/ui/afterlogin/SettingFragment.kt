package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSettingBinding
import com.futurecode.voicelockscreen.databinding.ItemSettingsRowBinding
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.ui.prelogin.LanguageFragment
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.utils.LockPermissionHelper
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore

/** Settings: Notifications + Language, Rate us / Feedback / Share app, Privacy policy. */
class SettingFragment : BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate) {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) openNotificationSettings()
        renderNotificationToggle()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VoiceNoteUi.lightStatusBar(this)
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        setupRows()
    }

    override fun onResume() {
        super.onResume()
        renderNotificationToggle()
        renderLockVoiceToggle()
    }

    private fun setupRows() {
        bindRow(binding.rowNotifications, R.drawable.ic_settings_bell, R.string.settings_notifications) {
            onNotificationsTapped()
        }
        binding.rowNotifications.switchRow.isVisible = true
        binding.rowNotifications.switchRow.setOnClickListener { onNotificationsTapped() }

        bindRow(binding.rowLockVoice, R.drawable.ic_settings_mic, R.string.home_lock_by_voice) { onLockVoiceTapped() }
        binding.rowLockVoice.switchRow.isVisible = true
        binding.rowLockVoice.switchRow.setOnClickListener { onLockVoiceTapped() }

        bindRow(binding.rowVoiceCommand, R.drawable.ic_camera_reset, R.string.change_voice_command) {
            findNavController().navigate(R.id.voiceSetupFragment)
        }
        bindRow(binding.rowLanguage, R.drawable.ic_settings_translate, R.string.settings_language) {
            findNavController().navigate(
                R.id.languageFragment,
                Bundle().apply { putBoolean(LanguageFragment.ARG_RETURN_TO_SETTINGS, true) }
            )
        }
        bindRow(binding.rowRate, R.drawable.ic_settings_star, R.string.settings_rate_us) {
            VoiceNoteUi.showRate(requireContext(), onNotReally = { sendFeedback() }, onLoveIt = { openStore() })
        }
        bindRow(binding.rowFeedback, R.drawable.ic_settings_feedback, R.string.settings_feedback) { sendFeedback() }
        bindRow(binding.rowShare, R.drawable.ic_settings_share, R.string.settings_share_app) { shareApp() }
        bindRow(binding.rowPrivacy, R.drawable.ic_settings_privacy, R.string.settings_privacy_policy) { openPrivacyPolicy() }
    }

    private fun bindRow(row: ItemSettingsRowBinding, @DrawableRes icon: Int, @StringRes title: Int, action: () -> Unit) {
        row.ivIcon.setImageResource(icon)
        row.tvTitle.setText(title)
        row.root.setOnClickListener { action() }
    }

    // region Lock screen by voice

    private fun renderLockVoiceToggle() {
        val row = bindingOrNull?.rowLockVoice ?: return
        row.switchRow.isChecked = VoiceLockSwitchStore.isLockEffective(requireContext())
    }

    private fun onLockVoiceTapped() {
        val context = requireContext()
        val enable = !VoiceLockSwitchStore.isLockEffective(context)
        VoiceLockSwitchStore.setLockEnabled(context, enable)
        when {
            enable && !SecureVoiceStore.hasPhrase(context) -> findNavController().navigate(R.id.voiceSetupFragment)
            enable && !VoiceLockSwitchStore.hasRequiredPermissions(context) -> findNavController().navigate(
                R.id.permissionFragment,
                Bundle().apply { putString(PermissionFragment.ARG_TARGET, PermissionFragment.TARGET_HOME) }
            )
        }
        renderLockVoiceToggle()
    }

    // endregion

    // region Notifications

    private fun notificationsEnabled(): Boolean =
        LockPermissionHelper.hasNotificationPermission(requireContext()) &&
            NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()

    private fun renderNotificationToggle() {
        val row = bindingOrNull?.rowNotifications ?: return
        row.switchRow.isChecked = notificationsEnabled()
    }

    private fun onNotificationsTapped() {
        val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !LockPermissionHelper.hasNotificationPermission(requireContext())
        if (needsRuntimePermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Turning notifications on or off after that happens in the system settings.
            openNotificationSettings()
        }
        renderNotificationToggle()
    }

    private fun openNotificationSettings() {
        openIntent(LockPermissionHelper.appNotificationSettingsIntent(requireContext()), getString(R.string.no_app_found))
    }

    // endregion

    // region Actions

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

    // endregion
}
