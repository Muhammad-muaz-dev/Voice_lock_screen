package com.futurecode.voicelockscreen.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurePinStore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "voice_lock_screen_pin_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS = "secure_pin_store"
    private const val KEY_PIN_CIPHER = "pin_cipher"
    private const val KEY_PIN_IV = "pin_iv"
    private const val KEY_PIN_LENGTH = "pin_length"
    private const val KEY_ATTEMPTS = "fail_attempts"
    private const val KEY_LOCKOUT_TIME = "lockout_timestamp"
    private const val GCM_TAG_LENGTH = 128
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30 * 60 * 1000L // 30 minutes

    fun savePin(context: Context, pin: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encryptedPin = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PIN_CIPHER, Base64.encodeToString(encryptedPin, Base64.NO_WRAP))
            .putString(KEY_PIN_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putInt(KEY_PIN_LENGTH, pin.length)
            .putInt(KEY_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_TIME, 0L)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Check if locked out
        val lockoutTime = prefs.getLong(KEY_LOCKOUT_TIME, 0L)
        if (lockoutTime > 0 && System.currentTimeMillis() < lockoutTime) {
            return false
        }

        val savedPin = decryptPin(context) ?: return false
        val isCorrect = savedPin == pin

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

    fun hasPin(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .contains(KEY_PIN_CIPHER)
    }

    fun getPinLength(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_PIN_LENGTH, 4)
            .let { if (it == 6) 6 else 4 }
    }

    private fun decryptPin(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val encryptedPin = prefs.getString(KEY_PIN_CIPHER, null) ?: return null
        val iv = prefs.getString(KEY_PIN_IV, null) ?: return null

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH, Base64.decode(iv, Base64.NO_WRAP))
        )
        val decryptedPin = cipher.doFinal(Base64.decode(encryptedPin, Base64.NO_WRAP))
        return String(decryptedPin, Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}
