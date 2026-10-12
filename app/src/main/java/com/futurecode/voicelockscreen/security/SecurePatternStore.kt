package com.futurecode.voicelockscreen.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object SecurePatternStore {
    private const val PREFS = "secure_pattern_store"
    private const val KEY_PATTERN_HASH = "pattern_hash"
    private const val KEY_PATTERN_SALT = "pattern_salt"
    private const val KEY_ATTEMPTS = "fail_attempts"
    private const val KEY_LOCKOUT_TIME = "lockout_timestamp"
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30 * 60 * 1000L // 30 minutes

    fun savePattern(context: Context, sequence: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(sequence, salt)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PATTERN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PATTERN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putInt(KEY_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_TIME, 0L)
            .apply()
    }

    fun verifyPattern(context: Context, sequence: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Check if locked out
        val lockoutTime = prefs.getLong(KEY_LOCKOUT_TIME, 0L)
        if (lockoutTime > 0 && System.currentTimeMillis() < lockoutTime) {
            return false
        }

        val savedHash = prefs.getString(KEY_PATTERN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PATTERN_SALT, null) ?: return false
        val candidateHash = hash(sequence, Base64.decode(salt, Base64.NO_WRAP))
        val isCorrect = MessageDigest.isEqual(Base64.decode(savedHash, Base64.NO_WRAP), candidateHash)

        if (isCorrect) {
            prefs.edit().putInt(KEY_ATTEMPTS, 0).putLong(KEY_LOCKOUT_TIME, 0L).apply()
        } else {
            val attempts = prefs.getInt(KEY_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_ATTEMPTS, attempts)
            if (attempts >= MAX_ATTEMPTS) {
                editor.putLong(KEY_LOCKOUT_TIME, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
            }
            editor.apply()
        }
        return isCorrect
    }

    fun getLockoutRemainingTime(context: Context): Long {
        val lockoutTime = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LOCKOUT_TIME, 0L)
        return (lockoutTime - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun hasPattern(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .contains(KEY_PATTERN_HASH)
    }

    private fun hash(sequence: String, salt: ByteArray): ByteArray {
        // Using a simple iterative hash to slow down brute force (simulating PBKDF2-like behavior without adding external deps)
        var result = salt + sequence.toByteArray(Charsets.UTF_8)
        repeat(10000) {
            result = MessageDigest.getInstance("SHA-256").digest(result)
        }
        return result
    }
}
