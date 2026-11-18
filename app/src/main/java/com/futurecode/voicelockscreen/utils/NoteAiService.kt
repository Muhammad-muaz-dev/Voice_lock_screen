package com.futurecode.voicelockscreen.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * AI actions for voice notes. The UI only talks to this interface, so the local
 * implementation can later be swapped for Firebase AI or our own server.
 */
interface NoteAiService {
    suspend fun summarize(text: String): Result<String>
    suspend fun translate(text: String, targetLanguageTag: String): Result<String>

    companion object {
        /** Same threshold as the reference app: notes shorter than this can't be summarized. */
        const val MIN_SUMMARY_CHARS = 200

        @Volatile
        var instance: NoteAiService = LocalNoteAiService
    }
}

class NoteAiUnavailableException : UnsupportedOperationException("No AI backend configured")

/** On-device fallback: extractive summary, no translation backend yet. */
object LocalNoteAiService : NoteAiService {
    private const val MIN_LOADING_MS = 900L

    override suspend fun summarize(text: String): Result<String> = withContext(Dispatchers.Default) {
        delay(MIN_LOADING_MS)
        runCatching { VoiceLockFeatureStore.summarize(text) }
    }

    override suspend fun translate(text: String, targetLanguageTag: String): Result<String> =
        withContext(Dispatchers.Default) {
            delay(MIN_LOADING_MS)
            Result.failure(NoteAiUnavailableException())
        }
}
