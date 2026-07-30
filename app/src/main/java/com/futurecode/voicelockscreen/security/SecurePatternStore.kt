package com.futurecode.voicelockscreen.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object SecurePatternStore {
    private const val PREFS = "secure_pattern_store"
    private const val KEY_PATTERN_HASH = "pattern_hash"
    private const val KEY_PATTERN_SALT = "pattern_salt"

    fun savePattern(context: Context, sequence: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(sequence, salt)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PATTERN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PATTERN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPattern(context: Context, sequence: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedHash = prefs.getString(KEY_PATTERN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PATTERN_SALT, null) ?: return false
        val candidateHash = hash(sequence, Base64.decode(salt, Base64.NO_WRAP))
        return MessageDigest.isEqual(Base64.decode(savedHash, Base64.NO_WRAP), candidateHash)
    }

    fun hasPattern(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .contains(KEY_PATTERN_HASH)
    }

    private fun hash(sequence: String, salt: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(
            salt + sequence.toByteArray(Charsets.UTF_8)
        )
    }
}
