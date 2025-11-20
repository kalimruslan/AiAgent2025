package ru.llm.agent.presentation.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.llm.agent.core.uikit.LlmAgentTheme

/**
 * Панель ввода сообщения с кнопками отправки и настроек
 */
@Composable
public fun InputBar(
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onSettingsClick: () -> Unit,
    text: String,
    onTextChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.padding(8.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSettingsClick,
            enabled = !isLoading
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = LlmAgentTheme.colors.onBackground)
        }
        TextField(
            value = text,
            onValueChange = {
                onTextChange(it)
            },
            placeholder = {
                Text(
                    "Ваше сообщение", color = LlmAgentTheme.colors.onSurface, fontSize = 16.sp
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f).fillMaxHeight().border(
                width = 2.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(16.dp)
            ),
            textStyle = TextStyle(fontSize = 16.sp)
        )
        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = {
                onSendMessage.invoke(text)
                onTextChange("") // Очищаем поле после отправки
            },
            enabled = !isLoading && text.isNotBlank()
        ) {
            Icon(modifier = Modifier.size(48.dp), imageVector = Icons.Filled.Send, contentDescription = null, tint = LlmAgentTheme.colors.onBackground)
        }
    }
}