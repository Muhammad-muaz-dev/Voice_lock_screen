package com.futurecode.voicelockscreen.ui.afterlogin

import android.app.Dialog
import android.content.Context
import android.text.format.DateFormat
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
import com.futurecode.voicelockscreen.databinding.VnSheetRateBinding
import com.futurecode.voicelockscreen.databinding.VnSheetSpeechLanguageBinding
import com.futurecode.voicelockscreen.databinding.VnItemSpeechLanguageBinding
import com.futurecode.voicelockscreen.databinding.VnToastBinding
import com.futurecode.voicelockscreen.utils.NoteTranslateLanguages
import com.futurecode.voicelockscreen.utils.VoiceLanguagePicker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared pieces of the AI Voice Notes screens (reference copy). */
object VoiceNoteUi {
    /**
     * The host activity paints white status-bar icons for the dark Home screen. These screens are light
     * (as in the reference), so they switch to dark icons while visible and put the previous look back on leave.
     */
    fun lightStatusBar(fragment: Fragment) {
        fragment.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            private var previous: Boolean? = null
            override fun onResume(owner: LifecycleOwner) {
                val window = fragment.activity?.window ?: return
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (previous == null) previous = controller.isAppearanceLightStatusBars
                controller.isAppearanceLightStatusBars = true
            }

            override fun onPause(owner: LifecycleOwner) {
                val window = fragment.activity?.window ?: return
                previous?.let { WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = it }
                previous = null
            }
        })
    }


    /** Reference screen transition: 300 ms slide, pop slides back. */
    val slideNavOptions: NavOptions
        get() = NavOptions.Builder()
            .setEnterAnim(R.anim.vn_enter_from_right)
            .setExitAnim(R.anim.vn_exit_to_left)
            .setPopEnterAnim(R.anim.vn_enter_from_left)
            .setPopExitAnim(R.anim.vn_exit_to_right)
            .build()

    fun formatDate(context: Context, time: Long): String {
        val locale = context.resources.configuration.locales[0]
        val skeleton = if (DateFormat.is24HourFormat(context)) "yMMMMdEEEEHm" else "yMMMMdEEEEhm"
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        return SimpleDateFormat(pattern, locale).format(Date(time))
    }

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

    /**
     * Bottom sheet that hugs its content like the reference. The sheet itself is transparent and gets no
     * system-bar padding (see VN.BottomSheet.Modal); the nav-bar inset is added once, to the white surface
     * [inset], so the white runs behind the gesture bar instead of leaving a gap (edge-to-edge on Android 15+).
     */
    internal fun transparentSheet(
        context: Context,
        root: View,
        inset: View = root,
        fullScreen: Boolean = false
    ): BottomSheetDialog {
        val dialog = BottomSheetDialog(context, R.style.VN_BottomSheet)
        dialog.setContentView(root)
        val basePadding = inset.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(inset) { v, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = basePadding + nav)
            insets
        }
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundResource(android.R.color.transparent)
            if (fullScreen && sheet != null) {
                // Edge-to-edge: the sheet is placed below the status bar, so MATCH_PARENT would push the
                // bottom (Apply button + its 42dp margin) under the screen edge. Size it to the space left.
                val parent = sheet.parent as View
                val top = ViewCompat.getRootWindowInsets(parent)
                    ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
                val height = parent.height - top
                sheet.layoutParams = sheet.layoutParams.apply {
                    this.height = if (height > 0) height else ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
            ViewCompat.requestApplyInsets(inset)
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }

    fun showMicAccess(context: Context, @StringRes descriptionRes: Int = R.string.vn_text_microphone_permission_desc, onOk: () -> Unit, onCancel: () -> Unit = {}) {
        val binding = VnSheetMicAccessBinding.inflate(LayoutInflater.from(context))
        val dialog = transparentSheet(context, binding.root, inset = binding.cl)
        binding.tvDesc.setText(descriptionRes)
        binding.tvCancel.setOnClickListener { dialog.dismiss(); onCancel() }
        binding.tvOK.setOnClickListener { dialog.dismiss(); onOk() }
        dialog.show()
        com.futurecode.voicelockscreen.utils.SystemBars.hide(dialog)
    }

    fun showDeleteNote(context: Context, onDelete: () -> Unit) {
        val binding = VnSheetDeleteNoteBinding.inflate(LayoutInflater.from(context))
        val dialog = transparentSheet(context, binding.root, inset = binding.cl)
        binding.tvCancel.setOnClickListener { dialog.dismiss() }
        binding.tvOK.setOnClickListener { dialog.dismiss(); onDelete() }
        dialog.show()
        com.futurecode.voicelockscreen.utils.SystemBars.hide(dialog)
    }

    /** "Did you enjoy using our app?" sheet from the reference Settings > Rate Us. */
    fun showRate(context: Context, onNotReally: () -> Unit, onLoveIt: () -> Unit) {
        val binding = VnSheetRateBinding.inflate(LayoutInflater.from(context))
        val dialog = transparentSheet(context, binding.root)
        binding.tvNotReally.setOnClickListener { dialog.dismiss(); onNotReally() }
        binding.tvLoveIt.setOnClickListener { dialog.dismiss(); onLoveIt() }
        dialog.show()
        com.futurecode.voicelockscreen.utils.SystemBars.hide(dialog)
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
        com.futurecode.voicelockscreen.utils.SystemBars.hide(dialog)
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

    /** Full-screen "Choose Your Language" sheet (translate target) with search and Apply. */
    fun showTranslateLanguages(context: Context, selectedTag: String, onApply: (String) -> Unit) {
        val binding = VnSheetTranslateLanguageBinding.inflate(LayoutInflater.from(context))
        val dialog = transparentSheet(context, binding.root, fullScreen = true)
        val all = NoteTranslateLanguages.options(context.resources.configuration.locales[0])
        var selected = selectedTag
        val adapter = LanguageAdapter(LanguageAdapter.Style.TRANSLATE) { tag -> selected = tag }
        adapter.submit(all, selected)
        binding.rcvLanguage.layoutManager = LinearLayoutManager(context)
        binding.rcvLanguage.addItemDecoration(SpaceDecoration(context.resources.getDimensionPixelSize(R.dimen.vn_dp_12)))
        binding.rcvLanguage.adapter = adapter
        all.indexOfFirst { it.first.equals(selected, true) }.takeIf { it > 0 }?.let { binding.rcvLanguage.scrollToPosition(it) }
        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim().orEmpty()
            val filtered = all.filter { it.second.contains(query, ignoreCase = true) }
            adapter.submit(filtered, selected)
            binding.rcvLanguage.isVisible = filtered.isNotEmpty()
            binding.llEmpty.isVisible = filtered.isEmpty()
        }
        binding.ivClose.setOnClickListener { dialog.dismiss() }
        binding.tvApply.setOnClickListener {
            dialog.dismiss()
            onApply(selected)
        }
        dialog.show()
        com.futurecode.voicelockscreen.utils.SystemBars.hide(dialog)
    }

    /** Full-screen speech-language sheet from the reference record screens; picking a row applies it. */
    fun showSpeechLanguages(context: Context, onSelected: () -> Unit) {
        val binding = VnSheetSpeechLanguageBinding.inflate(LayoutInflater.from(context))
        val dialog = transparentSheet(context, binding.root, fullScreen = true)
        val all = speechLanguages(context.resources.configuration.locales[0])
        val current = VoiceLanguagePicker.selected(context)
        val selected = all.firstOrNull { it.first.equals(current, true) }?.first
            ?: all.firstOrNull { Locale.forLanguageTag(it.first).language == Locale.forLanguageTag(current).language }?.first
            ?: current
        val adapter = LanguageAdapter(LanguageAdapter.Style.SPEECH) { tag ->
            VoiceLanguagePicker.save(context, tag)
            dialog.dismiss()
            onSelected()
        }
        adapter.submit(all, selected)
        binding.rcvLanguage.layoutManager = LinearLayoutManager(context)
        binding.rcvLanguage.adapter = adapter
        all.indexOfFirst { it.first.equals(selected, true) }.takeIf { it > 0 }?.let { binding.rcvLanguage.scrollToPosition(it) }
        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim().orEmpty()
            adapter.submit(all.filter { it.second.contains(query, ignoreCase = true) }, selected)
        }
        binding.ivClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
        com.futurecode.voicelockscreen.utils.SystemBars.hide(dialog)
    }

    private fun speechLanguages(displayLocale: Locale): List<Pair<String, String>> {
        val collator = Collator.getInstance(displayLocale)
        val locales = Locale.getAvailableLocales()
            .filter { it.language.isNotBlank() && it.variant.isBlank() && it.script.isBlank() }
            .map { it.toLanguageTag() to it.getDisplayName(displayLocale) }
            .filter { it.second.isNotBlank() }
            .distinctBy { it.second }
            .sortedWith { first, second -> collator.compare(first.second, second.second) }
        val english = locales.firstOrNull { it.first == "en-US" } ?: ("en-US" to Locale.US.getDisplayName(displayLocale))
        return listOf(english) + locales.filter { it.first != english.first }
    }

    /** Rows of both language sheets. The background is set from code, as in the reference, so its 16 dp padding applies. */
    private class LanguageAdapter(private val style: Style, private val onSelect: (String) -> Unit) :
        RecyclerView.Adapter<LanguageAdapter.Holder>() {
        enum class Style { TRANSLATE, SPEECH }

        private var items: List<Pair<String, String>> = emptyList()
        private var selected: String = ""

        class Holder(val root: View, val label: android.widget.TextView, val check: android.widget.ImageView) :
            RecyclerView.ViewHolder(root)

        fun submit(list: List<Pair<String, String>>, selectedTag: String) {
            items = list
            selected = selectedTag
            @Suppress("NotifyDataSetChanged")
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val inflater = LayoutInflater.from(parent.context)
            return if (style == Style.TRANSLATE) {
                val row = VnItemTranslateLanguageBinding.inflate(inflater, parent, false)
                Holder(row.root, row.tvLanguage, row.ivCheckBox)
            } else {
                val row = VnItemSpeechLanguageBinding.inflate(inflater, parent, false)
                Holder(row.root, row.tvLanguage, row.ivCheckBox)
            }
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val (tag, label) = items[position]
            val isSelected = tag == selected
            holder.label.text = label
            holder.check.setImageResource(if (isSelected) R.drawable.vn_ic_checked else R.drawable.vn_ic_check)
            holder.root.setBackgroundResource(
                if (isSelected) R.drawable.vn_bg_radius_16_stroke_primary01 else R.drawable.vn_bg_radius_16_neutral10
            )
            holder.root.setOnClickListener {
                if (style == Style.SPEECH) {
                    onSelect(tag)
                    return@setOnClickListener
                }
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
