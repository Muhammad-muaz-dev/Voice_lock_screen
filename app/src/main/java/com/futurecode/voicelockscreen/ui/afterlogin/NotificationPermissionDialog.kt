package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.DialogNotificationPermissionBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Home notification explanation; only Allow opens Android's permission prompt. */
class NotificationPermissionDialog : BottomSheetDialogFragment() {

    private var resultSent = false

    override fun getTheme() = R.style.Theme_VoiceLock_NotificationSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DialogNotificationPermissionBinding.inflate(inflater, container, false)
        setupActions(binding)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val sheetDialog = dialog as? BottomSheetDialog ?: return
        configureBehavior(sheetDialog)
        configureAppearance(sheetDialog)
    }

    override fun onCancel(dialog: DialogInterface) {
        sendResult(false)
        super.onCancel(dialog)
    }

    private fun setupActions(binding: DialogNotificationPermissionBinding) {
        binding.btnNotificationLater.setOnClickListener { respond(false) }
        binding.btnNotificationAllow.setOnClickListener { respond(true) }
    }

    private fun configureBehavior(sheetDialog: BottomSheetDialog) {
        sheetDialog.behavior.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun configureAppearance(sheetDialog: BottomSheetDialog) {
        sheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.apply {
                clearBackground(this)
                // Behavior installs its shape on the first layout.
                post { clearBackground(this) }
            }

        sheetDialog.window?.apply {
            navigationBarColor = requireContext().getColor(R.color.surfaceElevated)
            setDimAmount(SHEET_DIM_AMOUNT)
        }
    }

    private fun clearBackground(view: View) {
        view.backgroundTintList = null
        view.background = ColorDrawable(Color.TRANSPARENT)
    }

    private fun respond(allow: Boolean) {
        sendResult(allow)
        dismiss()
    }

    private fun sendResult(allow: Boolean) {
        if (resultSent) return
        resultSent = true

        val result = Bundle().apply { putBoolean(RESULT_ALLOW, allow) }
        parentFragmentManager.setFragmentResult(RESULT_KEY, result)
    }

    companion object {
        const val TAG = "home_notification_permission"
        const val RESULT_KEY = "notification_permission_choice"
        const val RESULT_ALLOW = "allow"
        private const val SHEET_DIM_AMOUNT = 0.62f
    }
}
