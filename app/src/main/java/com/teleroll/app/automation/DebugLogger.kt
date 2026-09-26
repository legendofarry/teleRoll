package com.teleroll.app.automation

import android.util.Log

object DebugLogger {
    private const val TAG = "teleRoll"

    @Volatile
    var enabled: Boolean = false

    fun d(message: String) {
        if (enabled) Log.d(TAG, message)
    }
}