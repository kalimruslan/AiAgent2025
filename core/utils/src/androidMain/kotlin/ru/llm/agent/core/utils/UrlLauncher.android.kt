package ru.llm.agent.core.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.AndroidRuntimeException
import androidx.core.net.toUri

public actual class UrlLauncher(
    context: PlatformContext,
) {
    private val appContext = context.applicationContext

    @Suppress("SwallowedException")
    public actual fun openUrl(
        url: String,
        context: PlatformContext?,
    ): Boolean {
        val contextForIntent = context ?: appContext
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        try {
            try {
                contextForIntent.startActivity(intent)
            } catch (e: AndroidRuntimeException) {
                contextForIntent.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (e: ActivityNotFoundException) {
            return false
        }
        return true
    }
}
