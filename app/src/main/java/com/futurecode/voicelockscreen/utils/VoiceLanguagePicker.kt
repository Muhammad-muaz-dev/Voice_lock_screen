package com.futurecode.voicelockscreen.utils

import android.content.Context
import com.futurecode.voicelockscreen.ui.afterlogin.VoiceNoteUi
import java.util.Locale

/** Speech-recognition language (stored as a BCP-47 tag). The picker is the reference "Choose Your Language" sheet. */
object VoiceLanguagePicker {
    private const val PREFS = "voice_language"
    private const val KEY = "locale"

    fun selected(context: Context): String = context.getSharedPreferences(PREFS, 0)
        .getString(KEY, Locale.getDefault().toLanguageTag()).orEmpty()

    fun label(context: Context): String =
        Locale.forLanguageTag(selected(context)).getDisplayName(context.resources.configuration.locales[0])

    fun save(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, tag).apply()
    }

    fun show(context: Context, onSelected: () -> Unit) = VoiceNoteUi.showSpeechLanguages(context, onSelected)
}
