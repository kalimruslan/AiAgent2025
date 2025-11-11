package ru.llm.agent.core.utils

import java.util.logging.Level
import java.util.logging.Logger as JavaLogger

private class DesktopLogger(private val tag: String) : Logger {
    private val javaLogger = JavaLogger.getLogger(tag)

    override fun info(message: String) {
        javaLogger.info(message)
    }

    override fun warning(message: String) {
        javaLogger.warning(message)
    }

    override fun error(message: String, throwable: Throwable?) {
        if (throwable != null) {
            javaLogger.log(Level.SEVERE, message, throwable)
        } else {
            javaLogger.severe(message)
        }
    }

    override fun debug(message: String) {
        javaLogger.fine(message)
    }
}

public actual fun createLogger(tag: String): Logger = DesktopLogger(tag)
