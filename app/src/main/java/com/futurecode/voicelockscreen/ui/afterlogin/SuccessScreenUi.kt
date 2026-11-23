package com.futurecode.voicelockscreen.ui.afterlogin

import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ViewSuccessScreenBinding

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
