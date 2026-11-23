package com.futurecode.voicelockscreen.utils

import android.content.res.ColorStateList
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.IncludeBottomTabsBinding

fun Fragment.bindBottomTabs(tabs: IncludeBottomTabsBinding, selected: Int) {
    floatBottomNav(tabs.root)
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
        // Selected tab: white pill with violet icon and bold violet label; others stay ink.
        tab.root.setBackgroundResource(
            if (isSelected) R.drawable.bg_bottom_tab_active else android.R.color.transparent
        )
        val tint = requireContext().getColor(if (isSelected) R.color.primary else R.color.primary_text)
        tab.icon.imageTintList = ColorStateList.valueOf(tint)
        tab.label.setTextColor(tint)
        tab.label.typeface = androidx.core.content.res.ResourcesCompat.getFont(
            requireContext(),
            if (isSelected) R.font.sora_bold else R.font.sora_medium
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


/** Keeps the tab bar above the system navigation area when the shared banner is hidden. */
private fun Fragment.floatBottomNav(nav: View) {
    val root = view ?: return
    val navParams = nav.layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return
    val baseNavMargin = navParams.bottomMargin

    val banner = activity?.findViewById<View>(R.id.sharedBannerSlot)

    root.fitsSystemWindows = false
    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
        val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        root.setPadding(root.paddingLeft, bars.top, root.paddingRight, 0)
        val bottomInset = if (banner?.visibility == View.VISIBLE) 0 else bars.bottom
        nav.layoutParams = navParams.apply { bottomMargin = baseNavMargin + bottomInset }
        insets
    }
    androidx.core.view.ViewCompat.requestApplyInsets(root)
}
