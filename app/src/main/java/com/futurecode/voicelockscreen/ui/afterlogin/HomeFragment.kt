package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentHomeBinding
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivSettings.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingFragment)
        }

        binding.voiceUnlock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_VOICE,
                R.id.action_homeFragment_to_voiceSetupFragment
            )
        }

        binding.pinLock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_PIN,
                R.id.action_homeFragment_to_pinLockFragment
            )
        }

        binding.patternLock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_PATTERN,
                R.id.action_homeFragment_to_patternLockFragment
            )
        }

        binding.fingerLock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_FINGERPRINT,
                R.id.action_homeFragment_to_fingerprintLockFragment
            )
        }

        binding.preview.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_screenPreviewFragment)
        }

        binding.tvSeeAllTools.setOnClickListener {

        }

        binding.tvSeeAllThemes.setOnClickListener {
         findNavController().navigate(R.id.action_homeFragment_to_seeAllthemesFragment)
        }

        bindVoiceLockSwitches()
        bindThemeCards()

        binding.ivPremium.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)

        }

        binding.ivMic.setOnClickListener {

        }
    }

    override fun onResume() {
        super.onResume()
        updateVoiceLockSwitches()
        updateThemeSelection()
    }

    private fun bindVoiceLockSwitches() {
        binding.swUnlock.setOnCheckedChangeListener { _, isChecked ->
            handleUnlockSwitchChanged(isChecked)
        }
        binding.swLock.setOnCheckedChangeListener { _, isChecked ->
            handleLockSwitchChanged(isChecked)
        }
        updateVoiceLockSwitches()
    }

    private fun handleUnlockSwitchChanged(enabled: Boolean) {
        VoiceLockSwitchStore.setUnlockEnabled(requireContext(), enabled)
        if (!enabled) {
            LockService.stop(requireContext())
            updateVoiceLockSwitches()
            return
        }

        if (!hasRequiredPermissions()) {
            openPermissionScreenForHomeSwitch()
            updateVoiceLockSwitches()
            return
        }

        LockService.startIfNeeded(requireContext())
        updateVoiceLockSwitches()
    }

    private fun handleLockSwitchChanged(enabled: Boolean) {
        VoiceLockSwitchStore.setLockEnabled(requireContext(), enabled)
        if (enabled && !hasRequiredPermissions()) {
            openPermissionScreenForHomeSwitch()
        }
        updateVoiceLockSwitches()
    }

    private fun updateVoiceLockSwitches() {
        val unlockChecked = VoiceLockSwitchStore.isUnlockEffective(requireContext())
        val lockChecked = VoiceLockSwitchStore.isLockEffective(requireContext())

        binding.swUnlock.setOnCheckedChangeListener(null)
        binding.swLock.setOnCheckedChangeListener(null)
        binding.swUnlock.isChecked = unlockChecked
        binding.swLock.isChecked = lockChecked
        binding.swUnlock.setOnCheckedChangeListener { _, isChecked ->
            handleUnlockSwitchChanged(isChecked)
        }
        binding.swLock.setOnCheckedChangeListener { _, isChecked ->
            handleLockSwitchChanged(isChecked)
        }

        binding.tvVoiceLockStatus.text = getString(
            if (unlockChecked || lockChecked) R.string.voice_lock_is_on else R.string.voice_lock_is_off
        )
    }

    private fun openPermissionScreenForHomeSwitch() {
        findNavController().navigate(
            R.id.action_homeFragment_to_permissionFragment,
            Bundle().apply {
                putString(PermissionFragment.ARG_TARGET, PermissionFragment.TARGET_HOME)
            }
        )
    }

    private fun bindThemeCards() {
        binding.cardThemeOne.setOnClickListener {
            saveTheme(ThemeWallpaperStore.THEME_ONE_ID, R.drawable.theme_one)
        }
        binding.cardThemeTwo.setOnClickListener {
            saveTheme(ThemeWallpaperStore.THEME_TWO_ID, R.drawable.theme_two)
        }
        binding.cardThemeThree.setOnClickListener {
            saveTheme(ThemeWallpaperStore.THEME_THREE_ID, R.drawable.theme_three)
        }
        updateThemeSelection()
    }

    private fun saveTheme(themeId: Int, wallpaperRes: Int) {
        ThemeWallpaperStore.saveSelectedTheme(requireContext(), themeId, wallpaperRes)
        updateThemeSelection()
    }

    private fun updateThemeSelection() {
        val selectedThemeId = ThemeWallpaperStore.getSelectedThemeId(requireContext())
        binding.ivThemeOneSelected.visibility =
            if (selectedThemeId == ThemeWallpaperStore.THEME_ONE_ID) View.VISIBLE else View.GONE
        binding.ivThemeTwoSelected.visibility =
            if (selectedThemeId == ThemeWallpaperStore.THEME_TWO_ID) View.VISIBLE else View.GONE
        binding.ivThemeThreeSelected.visibility =
            if (selectedThemeId == ThemeWallpaperStore.THEME_THREE_ID) View.VISIBLE else View.GONE
    }

    private fun openFeatureWithPermission(target: String, @IdRes directActionId: Int) {
        if (hasRequiredPermissions()) {
            findNavController().navigate(directActionId)
            return
        }

        findNavController().navigate(
            R.id.action_homeFragment_to_permissionFragment,
            Bundle().apply {
                putString(PermissionFragment.ARG_TARGET, target)
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return VoiceLockSwitchStore.hasRequiredPermissions(requireContext())
    }

}
