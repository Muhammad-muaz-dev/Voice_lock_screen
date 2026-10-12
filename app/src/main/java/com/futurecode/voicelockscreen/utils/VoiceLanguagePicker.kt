package com.futurecode.voicelockscreen.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.DialogVoiceLanguageBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

object VoiceLanguagePicker {
    fun selected(context: Context): String = context.getSharedPreferences("voice_language", 0)
        .getString("locale", Locale.getDefault().toLanguageTag()).orEmpty()

    fun label(context: Context): String = Locale.forLanguageTag(selected(context)).displayName

    fun show(context: Context, onSelected: () -> Unit) {
        val dialog = BottomSheetDialog(context)
        val binding = DialogVoiceLanguageBinding.inflate(LayoutInflater.from(context))
        val languages = Locale.getAvailableLocales().filter { it.language.isNotBlank() }
            .distinctBy { it.toLanguageTag() }.sortedBy { it.displayName }
        var filtered = languages
        fun refresh(query: String) {
            filtered = languages.filter { it.displayName.contains(query, true) || it.getDisplayName(it).contains(query, true) }
            binding.list.adapter = ArrayAdapter(context, R.layout.item_voice_language, filtered.map { it.displayName })
            binding.empty.visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
        binding.search.doAfterTextChanged { refresh(it.toString()) }
        binding.list.setOnItemClickListener { _, _, position, _ ->
            context.getSharedPreferences("voice_language", 0).edit()
                .putString("locale", filtered[position].toLanguageTag()).apply()
            onSelected()
            dialog.dismiss()
        }
        refresh("")
        dialog.setContentView(binding.root)
        dialog.show()
    }
}
