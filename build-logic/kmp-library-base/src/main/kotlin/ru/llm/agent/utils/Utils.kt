package ru.llm.agent.utils

import org.gradle.api.plugins.PluginContainer

/**
 * Apply plugin if it is not applied yet
 */
internal fun PluginContainer.applyIfNeeded(
    id: String,
    vararg ids: String,
): Boolean {
    if (hasPlugin(id) || ids.any(::hasPlugin)) return false

    apply(id)
    return true
}
