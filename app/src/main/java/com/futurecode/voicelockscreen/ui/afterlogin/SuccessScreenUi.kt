package com.futurecode.voicelockscreen.ui.afterlogin

import androidx.annotation.StringRes
import android.view.View
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.navigation.fragment.findNavController
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.databinding.ViewSuccessScreenBinding
import com.futurecode.voicelockscreen.service.LockService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun Fragment.showSetupSuccess(@StringRes announcement: Int, onComplete: () -> Unit) {
    val root = view ?: return
    if (root.getTag(R.id.successMark) != null) return
    val owner = viewLifecycleOwner
    val nav = findNavController()
    val entry = nav.currentBackStackEntry
    val dimLayer = ColorDrawable(ContextCompat.getColor(root.context, R.color.scrim))
    val animation = AnimatedVectorDrawableCompat.create(root.context, R.drawable.avd_pin_success)
    val mark = animation ?: requireNotNull(
        AppCompatResources.getDrawable(root.context, R.drawable.ic_pin_success_seal)
    )
    fun centerMark() {
        dimLayer.setBounds(0, 0, root.width, root.height)
        val size = minOf(mark.intrinsicWidth, root.width, root.height)
        val left = (root.width - size) / 2
        val top = (root.height - size) / 2
        mark.setBounds(left, top, left + size, top + size)
    }
    val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> centerMark() }
    root.setTag(R.id.successMark, mark)
    root.addOnLayoutChangeListener(layoutListener)
    centerMark()
    root.overlay.add(dimLayer)
    root.overlay.add(mark)
    root.announceForAccessibility(getString(announcement))
    animation?.start()
    owner.lifecycleScope.launch {
        try {
            delay(1_150L)
            owner.lifecycle.withResumed {
                if (nav.currentBackStackEntry === entry) {
                    val host = activity as? HomeActivity
                    if (host != null) host.finishSetupNavigation(onComplete) else onComplete()
                }
            }
        } finally {
            animation?.stop()
            root.overlay.remove(mark)
            root.overlay.remove(dimLayer)
            root.removeOnLayoutChangeListener(layoutListener)
            root.setTag(R.id.successMark, null)
        }
    }
}

internal fun Fragment.completeVoiceSetup() {
    LockService.enableAndStart(requireContext())
    showSetupSuccess(R.string.vn_text_congratulations) {
        findNavController().popBackStack(R.id.homeFragment, false)
    }
}

internal fun ViewSuccessScreenBinding.bindSuccess(
    @StringRes title: Int,
    @StringRes description: Int? = null,
    @StringRes action: Int = R.string.pin_done,
    onAction: () -> Unit
) {
    successTitle.setText(title)
    successDescription.isVisible = description != null
    description?.let { successDescription.setText(it) }
    successAction.setText(action)
    successAction.setOnClickListener { onAction() }
    val animation = AnimatedVectorDrawableCompat.create(root.context, R.drawable.avd_pin_success)
    if (animation != null) {
        successMark.setImageDrawable(animation)
        animation.start()
    } else {
        successMark.setImageResource(R.drawable.ic_pin_success_seal)
    }
}
