package ru.llm.agent.core.utils

import kotlin.jvm.JvmField

public actual abstract class PlatformContext private constructor() {
    public companion object {
        @JvmField
        public val INSTANCE: PlatformContext = object : PlatformContext() {}
    }
}
