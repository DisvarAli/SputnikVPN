package com.my.vpn.util

import android.util.Log
import com.my.vpn.BuildConfig

/** Структурированные логи этапов подключения (без секретов из shareLink). */
object ConnectLog {

    private const val TAG = "SputnikVPN.Connect"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(TAG, message, throwable) else Log.w(TAG, message)
    }

    fun stage(stage: String, detail: String? = null) {
        val line = if (detail.isNullOrBlank()) stage else "$stage: $detail"
        d(line)
    }
}
