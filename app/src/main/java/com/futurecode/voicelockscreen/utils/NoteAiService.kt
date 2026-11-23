package com.futurecode.voicelockscreen.utils

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * AI actions for voice notes. The UI only talks to this interface, so the summary backend can later be
 * swapped for a server without touching the screens.
 */
interface NoteAiService {
    suspend fun summarize(text: String): Result<String>

    /** [sourceHint] is used when the language of [text] can't be identified (e.g. the speech language). */
    suspend fun translate(text: String, targetLanguageTag: String, sourceHint: String = "en"): Result<String>

    companion object {
        /** Same threshold as the reference app: notes shorter than this can't be summarized. */
        const val MIN_SUMMARY_CHARS = 200

        @Volatile
        var instance: NoteAiService = LocalNoteAiService
    }
}

class NoteAiUnavailableException(message: String = "No AI backend configured") : UnsupportedOperationException(message)

/** On-device: extractive summary, ML Kit translation (models downloaded on demand). */
object LocalNoteAiService : NoteAiService {
    private const val MIN_LOADING_MS = 900L

    override suspend fun summarize(text: String): Result<String> = withContext(Dispatchers.Default) {
        delay(MIN_LOADING_MS)
        runCatching { VoiceLockFeatureStore.summarize(text) }
    }

    override suspend fun translate(text: String, targetLanguageTag: String, sourceHint: String): Result<String> =
        MlKitNoteTranslator.translate(text, targetLanguageTag, sourceHint)
}

/** Languages offered in the translate picker: only those ML Kit can translate, English first. */
object NoteTranslateLanguages {
    fun options(displayLocale: Locale): List<Pair<String, String>> {
        val collator = Collator.getInstance(displayLocale)
        val all = TranslateLanguage.getAllLanguages()
            .map { it to label(it, displayLocale) }
            .filter { it.second.isNotBlank() }
        val english = all.firstOrNull { it.first == TranslateLanguage.ENGLISH } ?: (TranslateLanguage.ENGLISH to label(TranslateLanguage.ENGLISH, displayLocale))
        return listOf(english) + all.filter { it.first != english.first }.sortedWith { first, second -> collator.compare(first.second, second.second) }
    }

    fun label(tag: String, displayLocale: Locale): String =
        Locale.forLanguageTag(tag).getDisplayName(displayLocale).ifBlank { tag }

    /** Maps any BCP-47 tag (e.g. "en-US", "zh-Hant") to an ML Kit code, or null if unsupported. */
    fun toMlKit(tag: String): String? {
        if (tag.isBlank() || tag == "und") return null
        TranslateLanguage.fromLanguageTag(tag)?.let { return it }
        val language = Locale.forLanguageTag(tag).language
        val normalized = when (language) {
            "iw" -> "he"
            "in" -> "id"
            "ji" -> "yi"
            "tl" -> "fil"
            "nb", "nn" -> "no"
            else -> language
        }
        return TranslateLanguage.fromLanguageTag(normalized)
    }
}

object MlKitNoteTranslator {
    private const val MAX_CHUNK = 900

    suspend fun translate(text: String, targetTag: String, sourceHint: String): Result<String> = runCatching {
        val target = NoteTranslateLanguages.toMlKit(targetTag)
            ?: throw NoteAiUnavailableException("Unsupported target language: $targetTag")
        val source = detectSource(text) ?: NoteTranslateLanguages.toMlKit(sourceHint) ?: TranslateLanguage.ENGLISH
        if (source == target) return@runCatching text
        val translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build()
        )
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            val out = StringBuilder()
            for (part in chunks(text)) {
                out.append(if (part.isBlank()) part else translatorTranslate(translator, part))
            }
            out.toString()
        } finally {
            translator.close()
        }
    }

    private suspend fun translatorTranslate(translator: com.google.mlkit.nl.translate.Translator, part: String): String {
        val lead = part.takeWhile { it.isWhitespace() }
        val trail = part.takeLastWhile { it.isWhitespace() }
        return lead + translator.translate(part.trim()).await() + trail
    }

    private suspend fun detectSource(text: String): String? {
        val client = LanguageIdentification.getClient()
        return try {
            val code = client.identifyLanguage(text.take(1000)).await()
            if (code == "und") null else NoteTranslateLanguages.toMlKit(code)
        } catch (e: Exception) {
            null
        } finally {
            client.close()
        }
    }

    /** Splits on line breaks, then sentences, so long notes stay within a comfortable request size. */
    private fun chunks(text: String): List<String> {
        val out = mutableListOf<String>()
        Regex("[^\\n]*\\n|[^\\n]+$").findAll(text).forEach { lineMatch ->
            val line = lineMatch.value
            if (line.length <= MAX_CHUNK) {
                out += line
            } else {
                val current = StringBuilder()
                Regex("[^.!?]+[.!?]*\\s*").findAll(line).forEach { sentence ->
                    if (current.length + sentence.value.length > MAX_CHUNK && current.isNotEmpty()) {
                        out += current.toString(); current.clear()
                    }
                    current.append(sentence.value)
                }
                if (current.isNotEmpty()) out += current.toString()
            }
        }
        return out
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
