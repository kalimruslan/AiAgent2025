package ru.llm.agent.core.utils

public operator fun <T> (() -> T).getValue(
    thisRef: Any,
    property: kotlin.reflect.KProperty<*>,
): T = this()
