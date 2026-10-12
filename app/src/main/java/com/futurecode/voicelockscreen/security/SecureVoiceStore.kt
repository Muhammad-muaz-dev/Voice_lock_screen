package com.futurecode.voicelockscreen.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.futurecode.voicelockscreen.ui.afterlogin.VoiceSetupFragment
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureVoiceStore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "voice_lock_screen_voice_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS = "secure_voice_store"
    private const val KEY_PHRASE_CIPHER = "voice_phrase_cipher"
    private const val KEY_PHRASE_IV = "voice_phrase_iv"
    private const val KEY_DISPLAY_PHRASE_CIPHER = "voice_display_phrase_cipher"
    private const val KEY_DISPLAY_PHRASE_IV = "voice_display_phrase_iv"
    private const val GCM_TAG_LENGTH = 128

    fun savePhrase(context: Context, phrase: String) {
        val normalizedPhrase = VoiceSetupFragment.normalizePhrase(phrase)

        // Encrypt verification phrase
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encryptedPhrase = cipher.doFinal(normalizedPhrase.toByteArray(Charsets.UTF_8))

        // Encrypt display phrase
        val displayCipher = Cipher.getInstance(TRANSFORMATION)
        displayCipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encryptedDisplayPhrase = displayCipher.doFinal(phrase.toByteArray(Charsets.UTF_8))

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PHRASE_CIPHER, Base64.encodeToString(encryptedPhrase, Base64.NO_WRAP))
            .putString(KEY_PHRASE_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_DISPLAY_PHRASE_CIPHER, Base64.encodeToString(encryptedDisplayPhrase, Base64.NO_WRAP))
            .putString(KEY_DISPLAY_PHRASE_IV, Base64.encodeToString(displayCipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun hasPhrase(context: Context): Boolean {
        return getNormalizedPhrase(context).isNotBlank()
    }

    fun getDisplayPhrase(context: Context): String {
        return decryptDisplayPhrase(context).ifBlank { legacyDisplayPhrase(context) }
    }

    fun getNormalizedPhrase(context: Context): String {
        return decryptPhrase(context) ?: legacyNormalizedPhrase(context)
    }

    fun verifyPhrase(context: Context, spokenPhrase: String): Boolean {
        val savedPhrase = getNormalizedPhrase(context)
        return savedPhrase.isNotBlank() && VoiceSetupFragment.normalizePhrase(spokenPhrase) == savedPhrase
    }

    private fun decryptPhrase(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val encryptedPhrase = prefs.getString(KEY_PHRASE_CIPHER, null) ?: return null
        val iv = prefs.getString(KEY_PHRASE_IV, null) ?: return null

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(encryptedPhrase, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun decryptDisplayPhrase(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val encryptedDisplay = prefs.getString(KEY_DISPLAY_PHRASE_CIPHER, null) ?: return ""
        val iv = prefs.getString(KEY_DISPLAY_PHRASE_IV, null) ?: return ""

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(encryptedDisplay, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrElse { "" }
    }

    private fun legacyDisplayPhrase(context: Context): String {
        return context
            .getSharedPreferences(VoiceSetupFragment.VOICE_PREFS, Context.MODE_PRIVATE)
            .getString(VoiceSetupFragment.KEY_VOICE_PHRASE, "")
            .orEmpty()
    }

    private fun legacyNormalizedPhrase(context: Context): String {
        return context
            .getSharedPreferences(VoiceSetupFragment.VOICE_PREFS, Context.MODE_PRIVATE)
            .getString(VoiceSetupFragment.KEY_VOICE_PHRASE_NORMALIZED, "")
            .orEmpty()
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
