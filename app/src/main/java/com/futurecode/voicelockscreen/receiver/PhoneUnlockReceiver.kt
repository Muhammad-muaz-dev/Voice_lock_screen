package com.futurecode.voicelockscreen.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.futurecode.voicelockscreen.security.AppLockState
import com.futurecode.voicelockscreen.service.LockMonitorService

class PhoneUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return
        if (!AppLockState.hasAnyLockEnabled(context)) return

        AppLockState.requestLockAfterPhoneUnlock(context)
        runCatching {
            LockMonitorService.showLockOverlay(context)
            Log.d(TAG, "Phone unlocked: requested lock overlay")
        }.onFailure {
            Log.e(TAG, "Phone unlocked but overlay launch failed", it)
        }
    }

    companion object {
        private const val TAG = "PhoneUnlockReceiver"
    }
}
