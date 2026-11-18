package com.futurecode.voicelockscreen.utils

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class VoiceNoteItem(
    val id: Long,
    val title: String,
    val transcript: String,
    val summary: String,
    val createdAt: Long,
    val durationSec: Int,
    val translation: String = "",
    val translateLanguage: String = ""
)

object VoiceLockFeatureStore {
    private const val PREFS = "voice_lock_features"
    private const val KEY_CAMERA_PHRASE = "camera_phrase"
    private const val KEY_CAMERA_ENABLED = "camera_enabled"
    private const val KEY_QUOTE = "lock_quote"
    private const val KEY_SHOW_CLOCK_QUOTE = "show_clock_quote"
    private const val KEY_CUSTOM_URI = "custom_theme_uri"
    private const val KEY_NOTES = "voice_notes"
    private const val KEY_TRIES = "voice_note_tries"
    private const val KEY_NOTIF_ASKED = "notif_asked"
    private const val DEFAULT_TRIES = 3
    private const val KEY_SUMMARY_TIP = "voice_note_summary_tip_shown"
    private const val KEY_TRANSLATE_LANGUAGE = "voice_note_translate_language"

    fun cameraPhrase(context: Context) =
        prefs(context).getString(KEY_CAMERA_PHRASE, "open camera").orEmpty()

    fun saveCameraPhrase(context: Context, phrase: String) {
        prefs(context).edit().putString(KEY_CAMERA_PHRASE, phrase.lowercase().trim()).apply()
    }

    fun isCameraEnabled(context: Context) = prefs(context).getBoolean(KEY_CAMERA_ENABLED, false)

    fun setCameraEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CAMERA_ENABLED, enabled).apply()
    }

    fun lockQuote(context: Context) =
        prefs(context).getString(KEY_QUOTE, "Every morning is a fresh start.").orEmpty()

    fun saveLockQuote(context: Context, quote: String) {
        prefs(context).edit().putString(KEY_QUOTE, quote).apply()
    }

    fun showClockAndQuote(context: Context) =
        prefs(context).getBoolean(KEY_SHOW_CLOCK_QUOTE, true)

    fun setShowClockAndQuote(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_CLOCK_QUOTE, show).apply()
    }

    fun customThemeUri(context: Context): Uri? =
        prefs(context).getString(KEY_CUSTOM_URI, null)?.let(Uri::parse)

    fun saveCustomThemeUri(context: Context, uri: Uri?) {
        prefs(context).edit().putString(KEY_CUSTOM_URI, uri?.toString()).apply()
    }

    fun remainingTries(context: Context) = prefs(context).getInt(KEY_TRIES, DEFAULT_TRIES)

    fun consumeTry(context: Context): Boolean {
        val left = remainingTries(context)
        if (left <= 0) return false
        prefs(context).edit().putInt(KEY_TRIES, left - 1).apply()
        return true
    }

    fun addTries(context: Context, amount: Int = 1) {
        prefs(context).edit().putInt(KEY_TRIES, remainingTries(context) + amount).apply()
    }

    fun notesAsked(context: Context) = prefs(context).getBoolean(KEY_NOTIF_ASKED, false)

    fun setNotifAsked(context: Context) {
        prefs(context).edit().putBoolean(KEY_NOTIF_ASKED, true).apply()
    }

    fun notes(context: Context): List<VoiceNoteItem> {
        val raw = prefs(context).getString(KEY_NOTES, "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            VoiceNoteItem(
                id = obj.getLong("id"),
                title = obj.getString("title"),
                transcript = obj.getString("transcript"),
                summary = obj.getString("summary"),
                createdAt = obj.getLong("createdAt"),
                durationSec = obj.optInt("durationSec"),
                translation = obj.optString("translation", ""),
                translateLanguage = obj.optString("translateLanguage", "")
            )
        }.sortedByDescending { it.createdAt }
    }

    fun saveNote(context: Context, note: VoiceNoteItem) {
        val list = notes(context).toMutableList()
        list.add(0, note)
        writeNotes(context, list)
    }

    fun note(context: Context, id: Long): VoiceNoteItem? = notes(context).firstOrNull { it.id == id }

    fun updateNote(context: Context, note: VoiceNoteItem) {
        val list = notes(context).map { if (it.id == note.id) note else it }
        writeNotes(context, list)
    }

    fun deleteNote(context: Context, id: Long) {
        writeNotes(context, notes(context).filterNot { it.id == id })
    }

    fun isSummaryTipShown(context: Context) = prefs(context).getBoolean(KEY_SUMMARY_TIP, false)

    fun setSummaryTipShown(context: Context) {
        prefs(context).edit().putBoolean(KEY_SUMMARY_TIP, true).apply()
    }

    fun translateLanguage(context: Context): String =
        prefs(context).getString(KEY_TRANSLATE_LANGUAGE, "en").orEmpty().ifBlank { "en" }

    fun saveTranslateLanguage(context: Context, tag: String) {
        prefs(context).edit().putString(KEY_TRANSLATE_LANGUAGE, tag).apply()
    }

    fun quoteForTopic(topic: String): String {
        return when (topic.lowercase()) {
            "passion" -> "Love what you do, and the rest follows."
            "growth" -> "Every day is a new chance to change your life."
            "vitality" -> "Take care of your body. It’s the only place you live."
            "aspire" -> "Aim higher than yesterday."
            "wisdom" -> "Learn a little, then live a little more."
            "courage" -> "Feel the fear, then take the step anyway."
            "harmony" -> "Quiet mind, steady heart."
            "gratitude" -> "Start with thanks, then start the day."
            "dreams" -> "Make the dream small enough to start today."
            else -> "Stay hopeful. The next page is still blank."
        }
    }

    fun summarize(transcript: String): String {
        val sentences = transcript.split(Regex("[.!?\\n]+"))
            .map { it.trim() }
            .filter { it.length > 8 }
            .take(4)
        if (sentences.isEmpty()) return transcript.take(80)
        return sentences.joinToString("\n") { "• $it" }
    }

    fun titleFrom(transcript: String): String {
        val first = transcript.trim().split(Regex("\\s+")).take(4).joinToString(" ")
        return first.ifBlank { "Note" }.replaceFirstChar { it.uppercase() }
    }

    private fun writeNotes(context: Context, list: List<VoiceNoteItem>) {
        val array = JSONArray()
        list.forEach { note ->
            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("title", note.title)
                    .put("transcript", note.transcript)
                    .put("summary", note.summary)
                    .put("createdAt", note.createdAt)
                    .put("durationSec", note.durationSec)
                    .put("translation", note.translation)
                    .put("translateLanguage", note.translateLanguage)
            )
        }
        prefs(context).edit().putString(KEY_NOTES, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
