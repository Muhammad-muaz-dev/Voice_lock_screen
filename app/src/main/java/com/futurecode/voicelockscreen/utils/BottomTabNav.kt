package com.futurecode.voicelockscreen.utils

import android.content.res.ColorStateList
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.IncludeBottomTabsBinding

fun Fragment.bindBottomTabs(tabs: IncludeBottomTabsBinding, selected: Int) {
    val selectedTab = selected.coerceIn(0, 2)
    val tabsByPosition = listOf(
        BottomTab(
            root = tabs.tabVoiceLock,
            iconContainer = tabs.tabVoiceLockIcon,
            icon = tabs.ivTabVoiceLock,
            label = tabs.tabVoiceLockLabel,
            labelResource = R.string.tab_voice_lock
        ),
        BottomTab(
            root = tabs.tabThemes,
            iconContainer = tabs.tabThemesIcon,
            icon = tabs.ivTabThemes,
            label = tabs.tabThemesLabel,
            labelResource = R.string.tab_themes
        ),
        BottomTab(
            root = tabs.tabVoiceNote,
            iconContainer = tabs.tabVoiceNoteIcon,
            icon = tabs.ivTabVoiceNote,
            label = tabs.tabVoiceNoteLabel,
            labelResource = R.string.tab_voice_note
        )
    )

    tabsByPosition.forEachIndexed { position, tab ->
        val isSelected = position == selectedTab
        tab.root.isSelected = isSelected
        tab.root.contentDescription = getString(tab.labelResource)
        tab.iconContainer.setBackgroundResource(
            if (isSelected) R.drawable.bg_bottom_tab_active else android.R.color.transparent
        )
        tab.icon.imageTintList = ColorStateList.valueOf(
            requireContext().getColor(
                if (isSelected) R.color.white else R.color.secondary_text
            )
        )
        tab.label.setTextColor(
            requireContext().getColor(
                if (isSelected) R.color.primary else R.color.secondary_text
            )
        )
        tab.root.setOnClickListener {
            when (position) {
                0 -> if (!isSelected) {
                    findNavController().popBackStack(R.id.homeFragment, false)
                }
                1 -> if (!isSelected) {
                    findNavController().navigate(R.id.themesFragment, null, navOptions {
                        popUpTo(R.id.homeFragment) { inclusive = false }
                        launchSingleTop = true
                    })
                }
                else -> if (!isSelected) {
                    findNavController().navigate(R.id.voiceNoteFragment, null, navOptions {
                        popUpTo(R.id.homeFragment) { inclusive = false }
                        launchSingleTop = true
                    })
                }
            }
        }
    }
}

private data class BottomTab(
    val root: View,
    val iconContainer: View,
    val icon: android.widget.ImageView,
    val label: android.widget.TextView,
    val labelResource: Int
)

fun View.gone() {
    visibility = View.GONE
}

fun View.visible() {
    visibility = View.VISIBLE
}
