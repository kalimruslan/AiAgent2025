package ru.llm.agent.core.utils

import java.awt.Desktop
import java.net.URI

public actual class UrlLauncher {
    public actual fun openUrl(
        url: String,
        context: PlatformContext?,
    ): Boolean {
        if (!Desktop.isDesktopSupported()) return false

        Desktop.getDesktop().browse(URI(url))
        return true
    }
}
