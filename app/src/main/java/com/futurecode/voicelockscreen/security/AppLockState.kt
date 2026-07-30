package com.futurecode.voicelockscreen.security

import android.content.Context
import com.futurecode.voicelockscreen.ui.afterlogin.FingerprintLockFragment
import com.futurecode.voicelockscreen.ui.afterlogin.ScreenPreviewFragment

object AppLockState {
    private const val PREFS = "lock_session_prefs"
    private const val KEY_LOCK_REQUESTED_AFTER_PHONE_UNLOCK = "lock_requested_after_phone_unlock"

    fun hasAnyLockEnabled(context: Context): Boolean {
        return SecurePinStore.hasPin(context) ||
            SecurePatternStore.hasPattern(context) ||
            SecureVoiceStore.hasPhrase(context) ||
            isFingerprintEnabled(context)
    }

    fun isUnlockedThisSession(context: Context): Boolean {
        return context
            .getSharedPreferences(ScreenPreviewFragment.LOCK_SESSION_PREFS, Context.MODE_PRIVATE)
            .getBoolean(ScreenPreviewFragment.KEY_UNLOCKED_THIS_SESSION, false)
    }

    fun setUnlockedThisSession(context: Context, unlocked: Boolean) {
        context
            .getSharedPreferences(ScreenPreviewFragment.LOCK_SESSION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ScreenPreviewFragment.KEY_UNLOCKED_THIS_SESSION, unlocked)
            .putBoolean(KEY_LOCK_REQUESTED_AFTER_PHONE_UNLOCK, false)
            .apply()
    }

    fun requestLockAfterPhoneUnlock(context: Context) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOCK_REQUESTED_AFTER_PHONE_UNLOCK, true)
            .putBoolean(ScreenPreviewFragment.KEY_UNLOCKED_THIS_SESSION, false)
            .apply()
    }

    fun consumePhoneUnlockRequest(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val requested = prefs.getBoolean(KEY_LOCK_REQUESTED_AFTER_PHONE_UNLOCK, false)
        if (requested) {
            prefs.edit()
                .putBoolean(KEY_LOCK_REQUESTED_AFTER_PHONE_UNLOCK, false)
                .apply()
        }
        return requested
    }

    private fun isFingerprintEnabled(context: Context): Boolean {
        return context
            .getSharedPreferences(FingerprintLockFragment.FINGERPRINT_PREFS, Context.MODE_PRIVATE)
            .getBoolean(FingerprintLockFragment.KEY_FINGERPRINT_ENABLED, false)
    }
}
