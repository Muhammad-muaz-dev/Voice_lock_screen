package com.futurecode.voicelockscreen.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object VoiceLockSwitchStore {
    private const val PREFS = "voice_lock_switch_prefs"
    private const val KEY_UNLOCK_ENABLED = "unlock_with_voice_enabled"
    private const val KEY_LOCK_ENABLED = "lock_with_voice_enabled"

    fun isUnlockEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_UNLOCK_ENABLED, false)
    }

    fun setUnlockEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_UNLOCK_ENABLED, enabled)
            .apply()
    }

    fun isLockEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOCK_ENABLED, false)
    }

    fun setLockEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOCK_ENABLED, enabled)
            .apply()
    }

    fun isUnlockEffective(context: Context): Boolean {
        return isUnlockEnabled(context) && hasRequiredPermissions(context) && com.futurecode.voicelockscreen.security.SecureVoiceStore.hasPhrase(context)
    }

    fun isLockEffective(context: Context): Boolean {
        return isLockEnabled(context) && hasRequiredPermissions(context) && com.futurecode.voicelockscreen.security.SecureVoiceStore.hasPhrase(context)
    }

    fun hasRequiredPermissions(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO) && hasLockPermissions(context)
    }

    fun hasLockPermissions(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.READ_PHONE_STATE) &&
            LockPermissionHelper.hasOverlayPermission(context)
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
