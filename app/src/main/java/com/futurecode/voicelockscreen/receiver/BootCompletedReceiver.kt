package com.futurecode.voicelockscreen.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.service.LockMonitorService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isBootEvent = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!isBootEvent) return
        if (!AppLockState.hasAnyLockEnabled(context)) return

        LockMonitorService.startIfNeeded(context)
    }
}
