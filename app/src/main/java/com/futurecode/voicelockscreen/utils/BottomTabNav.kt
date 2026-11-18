package com.futurecode.voicelockscreen.utils

import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.IncludeBottomTabsBinding

fun Fragment.bindBottomTabs(tabs: IncludeBottomTabsBinding, selected: Int) {
    val iconBg = R.drawable.bg_bottom_tab_active
    val white = requireContext().getColor(R.color.white)
    // Selected tab: white icon on the violet pill, violet label on the white bar.
    listOf(tabs.tabVoiceLockIcon, tabs.tabThemesIcon, tabs.tabVoiceNoteIcon).getOrNull(selected.coerceIn(0, 2))
        ?.let { (it.getChildAt(0) as? android.widget.ImageView)?.imageTintList = android.content.res.ColorStateList.valueOf(white) }
    when (selected) {
        0 -> {
            tabs.tabVoiceLockIcon.setBackgroundResource(iconBg)
            tabs.tabVoiceLockLabel.setTextColor(requireContext().getColor(R.color.primary))
        }
        1 -> {
            tabs.tabThemesIcon.setBackgroundResource(iconBg)
            tabs.tabThemesLabel.setTextColor(requireContext().getColor(R.color.primary))
        }
        else -> {
            tabs.tabVoiceNoteIcon.setBackgroundResource(iconBg)
            tabs.tabVoiceNoteLabel.setTextColor(requireContext().getColor(R.color.primary))
        }
    }
    tabs.tabVoiceLock.setOnClickListener {
        if (selected != 0) findNavController().popBackStack(R.id.homeFragment, false)
    }
    tabs.tabThemes.setOnClickListener {
        if (selected != 1) findNavController().navigate(R.id.themesFragment, null, navOptions {
            popUpTo(R.id.homeFragment) { inclusive = false }
            launchSingleTop = true
        })
    }
    tabs.tabVoiceNote.setOnClickListener {
        if (selected != 2) findNavController().navigate(R.id.voiceNoteFragment, null, navOptions {
            popUpTo(R.id.homeFragment) { inclusive = false }
            launchSingleTop = true
        })
    }
}

fun View.gone() { visibility = View.GONE }
fun View.visible() { visibility = View.VISIBLE }
