package com.futurecode.voicelockscreen.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.service.LockService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val shouldStart = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED ||
            intent.action == ACTION_RESTART_LOCK_SERVICE
        if (!shouldStart) return
        if (!AppLockState.hasAnyLockEnabled(context)) return

        LockService.startIfNeeded(context)
    }

    companion object {
        const val ACTION_RESTART_LOCK_SERVICE = "com.futurecode.voicelockscreen.RESTART_LOCK_SERVICE"
    }
}
