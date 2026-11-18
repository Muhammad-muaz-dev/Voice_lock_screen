package com.futurecode.voicelockscreen.ui.afterlogin

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.VnDialogNoteNameBinding
import com.futurecode.voicelockscreen.databinding.VnItemTranslateLanguageBinding
import com.futurecode.voicelockscreen.databinding.VnSheetDeleteNoteBinding
import com.futurecode.voicelockscreen.databinding.VnSheetMicAccessBinding
import com.futurecode.voicelockscreen.databinding.VnSheetTranslateLanguageBinding
import com.futurecode.voicelockscreen.databinding.VnToastBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared pieces of the AI Voice Notes screens (reference copy). */
object VoiceNoteUi {

    /** Reference screen transition: 300 ms slide, pop slides back. */
    val slideNavOptions: NavOptions
        get() = NavOptions.Builder()
            .setEnterAnim(R.anim.vn_enter_from_right)
            .setExitAnim(R.anim.vn_exit_to_left)
            .setPopEnterAnim(R.anim.vn_enter_from_left)
            .setPopExitAnim(R.anim.vn_exit_to_right)
            .build()

    fun formatDate(time: Long): String =
        SimpleDateFormat("EEEE, d MMMM yyyy, h:mma", Locale.ENGLISH).format(Date(time))

    fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    fun limitWords(text: String, max: Int): String {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (words.size <= max) text else words.take(max).joinToString(" ")
    }

    @Suppress("DEPRECATION")
    fun toast(context: Context, @StringRes message: Int, @DrawableRes icon: Int = R.drawable.vn_ic_success) {
        val binding = VnToastBinding.inflate(LayoutInflater.from(context))
        binding.ivThumb.setImageResource(icon)
        binding.tvMessage.setText(message)
        Toast(context.applicationContext).apply {
            duration = Toast.LENGTH_SHORT
            view = binding.root
            setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, context.resources.getDimensionPixelSize(R.dimen.vn_dp_80))
        }.show()
    }

    fun errorToast(context: Context, @StringRes message: Int) =
        toast(context, message, R.drawable.vn_ic_close_circle)

    private fun transparentSheet(context: Context, root: View, fullScreen: Boolean = false): BottomSheetDialog {
        val dialog = BottomSheetDialog(context, R.style.VN_BottomSheet)
        dialog.setContentView(root)
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundResource(android.R.color.transparent)
            if (fullScreen && sheet != null) {
                sheet.layoutParams = sheet.layoutParams.apply { height = ViewGroup.LayoutParams.MATCH_PARENT }
            }
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.behavior.skipCollapsed = true
        }
        return dialog
    }

    fun showMicAccess(context: Context, onOk: () -> Unit, onCancel: () -> Unit = {}) {
        val binding = VnSheetMicAccessBinding.inflate(LayoutInflater.from(context))
        val dialog = transparentSheet(context, binding.root)
        binding.tvCancel.setOnClickListener { dialog.dismiss(); onCancel() }
        binding.tvOK.setOnClickListener { dialog.dismiss(); onOk() }
        dialog.show()
    }

    fun showDeleteNote(context: Context, onDelete: () -> Unit) {
        val binding = VnSheetDeleteNoteBinding.inflate(LayoutInflater.from(context))
        val dialog = transparentSheet(context, binding.root)
        binding.tvCancel.setOnClickListener { dialog.dismiss() }
        binding.tvOK.setOnClickListener { dialog.dismiss(); onDelete() }
        dialog.show()
    }

    fun showNoteName(context: Context, current: String, onSave: (String) -> Unit) {
        val binding = VnDialogNoteNameBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context, R.style.VN_Dialog)
        dialog.setContentView(binding.root)
        dialog.window?.apply {
            setLayout(
                context.resources.displayMetrics.widthPixels - 2 * context.resources.getDimensionPixelSize(R.dimen.vn_dp_32),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        binding.etName.setText(current)
        binding.etName.setSelection(binding.etName.text?.length ?: 0)
        binding.tvSave.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim().orEmpty()
                .ifBlank { context.getString(R.string.vn_new_note) }
            dialog.dismiss()
            onSave(name)
        }
        dialog.show()
        binding.etName.requestFocus()
    }

    /** Anchored popup that drops down from [anchor], aligned to its end edge. */
    fun showPopup(anchor: View, content: View): PopupWindow {
        val popup = PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = anchor.resources.getDimension(R.dimen.vn_dp_10)
        popup.isOutsideTouchable = true
        content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        popup.showAsDropDown(anchor, anchor.width - content.measuredWidth, 0, Gravity.NO_GRAVITY)
        return popup
    }

    /** Swaps a background without letting the shape's own padding replace the view's padding. */
    fun setBackgroundKeepPadding(view: View, @DrawableRes background: Int) {
        val start = view.paddingStart
        val top = view.paddingTop
        val end = view.paddingEnd
        val bottom = view.paddingBottom
        view.setBackgroundResource(background)
        view.setPaddingRelative(start, top, end, bottom)
    }

    fun hideKeyboard(view: View) {
        (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showKeyboard(view: View) {
        view.requestFocus()
        (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun languageLabel(tag: String): String =
        Locale.forLanguageTag(tag).getDisplayName(Locale.ENGLISH).ifBlank { tag }

    /** Full-screen "Choose Your Language" sheet with search and Apply. */
    fun showTranslateLanguages(context: Context, selectedTag: String, onApply: (String) -> Unit) {
        val binding = VnSheetTranslateLanguageBinding.inflate(LayoutInflater.from(context))
        val dialog = transparentSheet(context, binding.root, fullScreen = true)
        val all = translateLanguages()
        var selected = selectedTag
        val adapter = LanguageAdapter { tag -> selected = tag }
        adapter.submit(all, selected)
        binding.rcvLanguage.layoutManager = LinearLayoutManager(context)
        binding.rcvLanguage.addItemDecoration(SpaceDecoration(context.resources.getDimensionPixelSize(R.dimen.vn_dp_12)))
        binding.rcvLanguage.adapter = adapter
        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim().orEmpty()
            val filtered = all.filter { it.second.contains(query, ignoreCase = true) }
            adapter.submit(filtered, selected)
            val empty = filtered.isEmpty()
            binding.rcvLanguage.isVisible = !empty
            binding.tvNoLanguage.isVisible = empty
            binding.tvNoLanguageDesc.isVisible = empty
        }
        binding.ivClose.setOnClickListener { dialog.dismiss() }
        binding.tvApply.setOnClickListener {
            dialog.dismiss()
            onApply(selected)
        }
        dialog.show()
    }

    private fun translateLanguages(): List<Pair<String, String>> {
        val tags = listOf(
            "en", "af", "sq", "ar", "bn", "be", "bg", "ca", "zh", "hr", "cs", "da", "nl", "eo", "et", "fil",
            "fi", "fr", "gl", "ka", "de", "el", "gu", "he", "hi", "hu", "is", "id", "ga", "it", "ja", "kn",
            "ko", "lv", "lt", "mk", "ms", "mt", "mr", "no", "fa", "pl", "pt", "ro", "ru", "sk", "sl", "es",
            "sw", "sv", "ta", "te", "th", "tr", "uk", "ur", "vi", "cy"
        )
        val english = tags.first()
        val rest = tags.drop(1).map { it to languageLabel(it) }.sortedBy { it.second }
        return listOf(english to languageLabel(english)) + rest
    }

    private class LanguageAdapter(private val onSelect: (String) -> Unit) :
        RecyclerView.Adapter<LanguageAdapter.Holder>() {
        private var items: List<Pair<String, String>> = emptyList()
        private var selected: String = ""

        class Holder(val row: VnItemTranslateLanguageBinding) : RecyclerView.ViewHolder(row.root)

        fun submit(list: List<Pair<String, String>>, selectedTag: String) {
            items = list
            selected = selectedTag
            @Suppress("NotifyDataSetChanged")
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(VnItemTranslateLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val (tag, label) = items[position]
            val isSelected = tag == selected
            holder.row.tvLanguage.text = label
            holder.row.root.isSelected = isSelected
            holder.row.ivCheckBox.setImageResource(if (isSelected) R.drawable.vn_ic_checked else R.drawable.vn_ic_check)
            holder.row.root.setOnClickListener {
                if (selected == tag) return@setOnClickListener
                selected = tag
                onSelect(tag)
                @Suppress("NotifyDataSetChanged")
                notifyDataSetChanged()
            }
        }
    }

    class SpaceDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            if (parent.getChildAdapterPosition(view) > 0) outRect.top = space
        }
    }
}
