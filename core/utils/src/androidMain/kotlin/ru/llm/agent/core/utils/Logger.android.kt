package ru.llm.agent.core.utils

import android.util.Log

private class AndroidLogger(private val tag: String) : Logger {
    override fun info(message: String) {
        Log.i(tag, message)
    }

    override fun warning(message: String) {
        Log.w(tag, message)
    }

    override fun error(message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    override fun debug(message: String) {
        Log.d(tag, message)
    }
}

public actual fun createLogger(tag: String): Logger = AndroidLogger(tag)
