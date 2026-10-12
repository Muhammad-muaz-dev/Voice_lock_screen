package com.futurecode.voicelockscreen.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LockScreenViewModel : ViewModel() {

    private val _unlockStatus = MutableLiveData<UnlockStatus>()
    val unlockStatus: LiveData<UnlockStatus> = _unlockStatus

    private val _lockoutTime = MutableLiveData<Long>()
    val lockoutTime: LiveData<Long> = _lockoutTime

    sealed class UnlockStatus {
        object Success : UnlockStatus()
        data class Failure(val messageRes: Int) : UnlockStatus()
        data class Lockout(val remainingTimeMs: Long) : UnlockStatus()
    }

    fun verifyPin(context: Context, pin: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val isCorrect = SecurePinStore.verifyPin(context, pin)
                if (isCorrect) {
                    UnlockStatus.Success
                } else {
                    val remaining = SecurePinStore.getLockoutRemainingTime(context)
                    if (remaining > 0) {
                        UnlockStatus.Lockout(remaining)
                    } else {
                        UnlockStatus.Failure(com.futurecode.voicelockscreen.R.string.pin_wrong)
                    }
                }
            }
            _unlockStatus.value = result
        }
    }

    fun verifyPattern(context: Context, sequence: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val isCorrect = SecurePatternStore.verifyPattern(context, sequence)
                if (isCorrect) {
                    UnlockStatus.Success
                } else {
                    val remaining = SecurePatternStore.getLockoutRemainingTime(context)
                    if (remaining > 0) {
                        UnlockStatus.Lockout(remaining)
                    } else {
                        UnlockStatus.Failure(com.futurecode.voicelockscreen.R.string.pattern_wrong)
                    }
                }
            }
            _unlockStatus.value = result
        }
    }

    fun verifyVoice(context: Context, spoken: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (SecureVoiceStore.verifyPhrase(context, spoken)) {
                    UnlockStatus.Success
                } else {
                    UnlockStatus.Failure(com.futurecode.voicelockscreen.R.string.voice_try_again)
                }
            }
            _unlockStatus.value = result
        }
    }
}
