package com.futurecode.voicelockscreen.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import com.futurecode.voicelockscreen.databinding.DialogConfirmDarkBinding

object DarkDialogs {
    fun show(
        context: Context,
        title: String,
        body: String,
        negative: String,
        positive: String,
        onNegative: () -> Unit = {},
        onPositive: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogConfirmDarkBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.tvDialogTitle.text = title
        binding.tvDialogBody.text = body
        binding.btnDialogNegative.text = negative
        binding.btnDialogPositive.text = positive
        binding.btnDialogNegative.setOnClickListener {
            dialog.dismiss()
            onNegative()
        }
        binding.btnDialogPositive.setOnClickListener {
            dialog.dismiss()
            onPositive()
        }
        dialog.show()
        dialog.window?.apply {
            setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(.62f)
        }
    }
}
