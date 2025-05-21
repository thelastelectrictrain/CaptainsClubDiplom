package com.example.captainsclub2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ReportViewDialog(
    reportText: String,
    onExportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Расширяем на весь экран
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Отчёт о смене",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Основной контент с прокруткой
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val scrollState = rememberScrollState()

                    Text(
                        text = reportText,
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp // Увеличиваем межстрочный интервал
                        )
                    )

                    // Индикатор прокрутки (опционально)
//                    VerticalScrollbar(
//                        modifier = Modifier
//                            .align(LineHeightStyle.Alignment.CenterEnd)
//                            .fillMaxHeight(),
//                        adapter = rememberScrollbarAdapter(scrollState)
//                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки внизу диалога
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Закрыть")
                    }

                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D47A1)
                        )
                    ) {
                        Text("Экспорт")
                    }
                }
            }
        }
    }
}