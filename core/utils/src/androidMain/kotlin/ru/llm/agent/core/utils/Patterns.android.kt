package ru.llm.agent.core.utils

import androidx.core.util.PatternsCompat

public actual fun validateEmail(email: String): Boolean {
    return PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()
}
