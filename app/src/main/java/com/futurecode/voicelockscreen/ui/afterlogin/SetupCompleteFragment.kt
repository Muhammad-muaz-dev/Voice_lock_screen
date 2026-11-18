package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.adapter.ThemeAdapter
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSetupCompleteBinding
import com.futurecode.voicelockscreen.databinding.VnItemBackupLockBinding
import com.futurecode.voicelockscreen.model.ThemeModel
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore

/** "Congratulations!" screen (reference copy): backup unlock methods, preview card, theme strip. */
class SetupCompleteFragment : BaseFragment<FragmentSetupCompleteBinding>(FragmentSetupCompleteBinding::inflate) {

    private data class Backup(@DrawableRes val icon: Int, @StringRes val title: Int, @IdRes val destination: Int)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VoiceNoteUi.lightStatusBar(this)
        LockService.enableAndStart(requireContext())
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        binding.ivClose.setOnClickListener { goHome() }
        binding.framePreview.setOnClickListener { findNavController().navigate(R.id.screenPreviewFragment) }
        binding.tvSeeAll.setOnClickListener { findNavController().navigate(R.id.seeAllthemesFragment) }
        binding.rcvBackupLock.layoutManager = GridLayoutManager(requireContext(), 3)
        setupThemes()
    }

    override fun onResume() {
        super.onResume()
        val context = requireContext()
        val fingerprintOn = context.getSharedPreferences(FingerprintLockFragment.FINGERPRINT_PREFS, 0)
            .getBoolean(FingerprintLockFragment.KEY_FINGERPRINT_ENABLED, false)
        val items = buildList {
            if (!SecurePinStore.hasPin(context)) add(Backup(R.drawable.vn_ic_backup_pin, R.string.vn_backup_pin, R.id.pinLockFragment))
            if (!SecurePatternStore.hasPattern(context)) add(Backup(R.drawable.vn_ic_backup_pattern, R.string.vn_backup_pattern, R.id.patternLockFragment))
            if (!fingerprintOn) add(Backup(R.drawable.vn_ic_backup_fingerprint, R.string.vn_backup_fingerprint, R.id.fingerprintLockFragment))
        }
        binding.rcvBackupLock.isVisible = items.isNotEmpty()
        binding.rcvBackupLock.adapter = BackupAdapter(items) { findNavController().navigate(it.destination) }
    }

    private fun setupThemes() {
        val themes = listOf(
            ThemeModel(ThemeWallpaperStore.THEME_ONE_ID, R.drawable.theme_one),
            ThemeModel(ThemeWallpaperStore.THEME_TWO_ID, R.drawable.theme_two),
            ThemeModel(ThemeWallpaperStore.THEME_THREE_ID, R.drawable.theme_three),
            ThemeModel(ThemeWallpaperStore.THEME_FOUR_ID, R.drawable.theme_four),
            ThemeModel(ThemeWallpaperStore.THEME_FIVE_ID, R.drawable.theme_five),
            ThemeModel(ThemeWallpaperStore.THEME_SIX_ID, R.drawable.theme_six)
        )
        lateinit var adapter: ThemeAdapter
        adapter = ThemeAdapter(themes, ThemeWallpaperStore.getSelectedThemeId(requireContext())) { theme ->
            ThemeWallpaperStore.saveSelectedTheme(requireContext(), theme.id, theme.imageResId)
            adapter.setSelectedTheme(theme.id)
            Toast.makeText(requireContext(), R.string.theme_applied, Toast.LENGTH_SHORT).show()
        }
        binding.rcvTheme.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rcvTheme.adapter = adapter
    }

    private fun goHome() {
        findNavController().popBackStack(R.id.homeFragment, false)
    }

    private class BackupAdapter(private val items: List<Backup>, private val onClick: (Backup) -> Unit) :
        RecyclerView.Adapter<BackupAdapter.Holder>() {
        class Holder(val row: VnItemBackupLockBinding) : RecyclerView.ViewHolder(row.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(VnItemBackupLockBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.row.ivThumb.setImageResource(item.icon)
            holder.row.tvTitle.setText(item.title)
            holder.row.root.setOnClickListener { onClick(item) }
        }
    }
}
