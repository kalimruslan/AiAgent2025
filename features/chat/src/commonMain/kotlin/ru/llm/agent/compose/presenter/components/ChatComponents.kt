package ru.llm.agent.compose.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.llm.agent.compose.presenter.AiType
import ru.llm.agent.model.PromtFormat

@Composable
internal fun SelectLlmDropDown(
    selectedAiType: AiType, onApiSelected: (AiType) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }

    Box {
        Text(
            modifier = Modifier.padding(top = 8.dp).clickable { expanded.value = true },
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            text = selectedAiType.displayName
        )

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            modifier = Modifier.background(Color(0xFF232533), shape = RoundedCornerShape(16.dp))
                .shadow(8.dp, RoundedCornerShape(20.dp))
        ) {
            AiType.values().forEach { aiType ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = aiType.displayName,
                            color = Color.Green,
                            modifier = Modifier.padding(4.dp)
                        )
                    }, onClick = {
                        onApiSelected(aiType)
                        expanded.value = false
                    }, modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun SelectOutputFormatDropDown(
    selectedOutputFormat: PromtFormat,
    onOutputFormatSelect: (PromtFormat) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }

    Box {
        Text(
            modifier = Modifier.padding(top = 8.dp).clickable { expanded.value = true },
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            text = selectedOutputFormat.name
        )

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            modifier = Modifier.background(Color(0xFF232533), shape = RoundedCornerShape(16.dp))
                .shadow(8.dp, RoundedCornerShape(20.dp))
        ) {
            PromtFormat.entries.forEach { format ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = format.name,
                            color = Color.Green,
                            modifier = Modifier.padding(4.dp)
                        )
                    }, onClick = {
                        onOutputFormatSelect(format)
                        expanded.value = false
                    }, modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}