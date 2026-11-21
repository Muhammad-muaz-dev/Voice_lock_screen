package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.DialogNotificationPermissionBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Home notification explanation styled like the app's microphone-access sheet. */
class NotificationPermissionDialog : BottomSheetDialogFragment() {

    private var resultSent = false
    private var notificationSurface: View? = null

    override fun getTheme() = R.style.VN_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DialogNotificationPermissionBinding.inflate(inflater, container, false)
        notificationSurface = binding.notificationBody
        val baseBottomPadding = binding.notificationBody.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.notificationBody) { view, insets ->
            val navigationInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(bottom = baseBottomPadding + navigationInset)
            insets
        }
        setupActions(binding)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val sheetDialog = dialog as? BottomSheetDialog ?: return
        configureBehavior(sheetDialog)
        configureAppearance(sheetDialog)
        notificationSurface?.let { ViewCompat.requestApplyInsets(it) }
    }

    override fun onCancel(dialog: DialogInterface) {
        sendResult(false)
        super.onCancel(dialog)
    }

    override fun onDestroyView() {
        notificationSurface = null
        super.onDestroyView()
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
            ?.let { sheet ->
                sheet.setBackgroundResource(android.R.color.transparent)
                sheet.post { sheet.setBackgroundResource(android.R.color.transparent) }
            }
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
    }
}