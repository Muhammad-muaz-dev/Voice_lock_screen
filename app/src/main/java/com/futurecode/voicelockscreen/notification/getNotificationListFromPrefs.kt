package com.futurecode.voicelockscreen.notification

import android.util.Log
import com.futurecode.voicelockscreen.activity.MyApplication
import com.google.gson.Gson

fun getNotificationListFromPrefs(): List<NotificationModel> {
    return try {
        val jsonString = MyApplication.app.prefManager.notificationList

        Log.d("TAG", "getNotificationListFromPrefs: $jsonString")
        if (jsonString.isNullOrEmpty()) return emptyList()

        Gson().fromJson(
            jsonString,
            Array<NotificationModel>::class.java
        )?.toList() ?: emptyList()

    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
