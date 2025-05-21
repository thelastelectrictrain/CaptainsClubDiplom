package com.example.captainsclub2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun NumberSelectionMenu(
    itemType: String,
    occupiedNumbers: List<Int>,
    isReplacementMode: Boolean = false, // Новый параметр
    onNumbersSelected: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedNumbers by remember { mutableStateOf(setOf<Int>()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Выберите номер $itemType",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                val numberGroups = (1..99).chunked(10)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    numberGroups.forEach { group ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            group.forEach { number ->
                                val isSelected = selectedNumbers.contains(number)
                                val isOccupied = occupiedNumbers.contains(number)

                                Box(
                                    modifier = Modifier.padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(

//                                        onClick = {
//                                            selectedNumbers = if (isSelected) {
//                                                selectedNumbers - number
//                                            } else {
//                                                selectedNumbers + number
//                                            }
//                                        },
                                        onClick = {
                                            if (isReplacementMode) {
                                                // В режиме замены можно выбрать только один номер
                                                selectedNumbers = if (isSelected) emptySet() else setOf(number)
                                            } else {
                                                selectedNumbers = if (isSelected) {
                                                    selectedNumbers - number
                                                } else {
                                                    selectedNumbers + number
                                                }
                                            }
                                        },
                                        //modifier = Modifier.size(60.dp), // Увеличили размер кнопки
                                        modifier = Modifier
                                            .width(90.dp) // Ширина кнопки
                                            .height(50.dp), // Высота кнопки
                                        shape = RoundedCornerShape(14.dp), // Закругленные углы
                                        enabled = !isOccupied,
//                                        colors = ButtonDefaults.buttonColors(
//                                            containerColor = Color(0xFF0D47A1),
//                                            disabledContainerColor = Color.Gray,
//                                            contentColor = Color.White
//                                        )
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when {
                                                isOccupied -> Color.Gray
                                                isSelected -> Color.Green
                                                else -> Color(0xFF0D47A1)
                                            }
                                        )
                                    ) {
                                        Text(
                                            text = "$number",
                                            fontSize = 10.sp, // Увеличили текст
                                            maxLines = 1,
                                            overflow = TextOverflow.Visible
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка подтверждения
                Button(
                    onClick = {
                        onNumbersSelected(selectedNumbers.toList())
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedNumbers.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D47A1)
                    )
                ) {
                    Text("Подтвердить выбор (${selectedNumbers.size})")
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF0D47A1) // Ваш фирменный цвет
                    )
                ) {
                    Text("Отмена", fontSize = 16.sp)
                }
            }
        }
    }
}