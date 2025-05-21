package com.example.captainsclub2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.example.captainsclub2.data.models.Operation
import com.example.captainsclub2.ui.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


@Composable
fun GroupedRentalSettingsDialog(
    operations: List<Operation>,
    onUpdate: (Operation) -> Unit,
    onDismiss: () -> Unit,
    onExtendRental: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    // Используем настройки из первой операции как общие для всей группы
    val firstOperation = operations.first()

    // Состояния для чекбоксов
    var isCash by remember { mutableStateOf(firstOperation.isCash) }
    var hasDiscount by remember { mutableStateOf(firstOperation.hasDiscount) }

    // Общий комментарий (можно оставить или удалить, в зависимости от потребностей)
    var comment by remember { mutableStateOf(firstOperation.comment) }

    var saveJob by remember { mutableStateOf<Job?>(null) }

    //для замены номера

    var showReplaceDialog by remember { mutableStateOf(false) }
    var showNumberSelection by remember { mutableStateOf(false) }
    var selectedOpForReplacement by remember { mutableStateOf<Operation?>(null) }

    // Получаем groupId безопасно
    val groupId by remember(operations) {
        derivedStateOf { operations.firstOrNull()?.groupId ?: "" }
    }

    // Получаем операции через collectAsState()
    val allOperations by viewModel.operations.collectAsState()



    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки групповой аренды") },
        text = {
            Column {

                // Список номеров в группе
                Text(
                    text = "${if (firstOperation.itemType == "DUCK") "Утки" else "Яхты"} ${operations.joinToString { it.itemId.toString() }}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Поле для комментария
                OutlinedTextField(
                    value = comment,
                    onValueChange = { newComment ->
                        comment = newComment
                        saveJob?.cancel()
                        saveJob = viewModel.viewModelScope.launch {
                            delay(500) // Ждем 500ms после последнего изменения
                            viewModel.updateOperation(
                                firstOperation.copy(
                                    comment = newComment
                                )
                            )
                        }
                    },
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Общие настройки
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isCash,
                        onCheckedChange = { isChecked ->
                            isCash = isChecked
                            // Применяем ко всем операциям в группе
                            operations.forEach { op ->
                                onUpdate(op.copy(isCash = isChecked))
                            }
                        }
                    )
                    Text("Наличка")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasDiscount,
                        onCheckedChange = { isChecked ->
                            hasDiscount = isChecked
                            val newPrice = if (isChecked) 175.0 else 350.0
                            // Применяем ко всем операциям в группе
                            operations.forEach { op ->
                                onUpdate(op.copy(
                                    hasDiscount = isChecked,
                                    price = newPrice
                                ))
                            }
                        }
                    )
                    Text("Скидка")
                }

                // Бейдж с количеством продлений
                if (firstOperation.extensionsCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Badge(
                        containerColor = Color(0xFFFFA000),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("+${firstOperation.extensionsCount * 20} мин")
                    }
                }

                Button(
                    onClick = {
                        // Показываем сначала выбор какой номер заменить
                        showReplaceDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA000)
                    )
                ) {
                    Text("Заменить номер в группе")
                }

                if (showReplaceDialog) {
                    AlertDialog(
                        onDismissRequest = { showReplaceDialog = false },
                        title = { Text("Выберите номер для замены") },
                        text = {
                            Column {
                                operations.forEach { op ->
                                    Button(
                                        onClick = {
                                            selectedOpForReplacement = op
                                            showReplaceDialog = false
                                            showNumberSelection = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("${if (op.itemType == "DUCK") "Утка" else "Яхта"} ${op.itemId}")
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showReplaceDialog = false },
                                colors = ButtonDefaults.buttonColors()
                            ) {
                                Text("Отмена")
                            }
                        }
                    )
                }

                if (showNumberSelection && selectedOpForReplacement != null) {
                    val occupiedNumbers by remember(operations, allOperations) {
                        derivedStateOf {
                            // Получаем тип заменяемой игрушки
                            val currentType = selectedOpForReplacement?.itemType ?: ""

                            allOperations
                                .filter { op ->
                                    op.type == "RENTAL" &&
                                            op.status == "ACTIVE" &&
                                            op.itemType == currentType // Фильтруем только тот же тип
                                }
                                .map { it.itemId } +
                                    operations
                                        .filter { it.itemType == currentType }
                                        .map { it.itemId }
                        }
                    }

                    val filteredOccupied by remember(occupiedNumbers, selectedOpForReplacement) {
                        derivedStateOf {
                            selectedOpForReplacement?.let { selectedOp ->
                                occupiedNumbers.filterNot { it == selectedOp.itemId }
                            } ?: emptyList()
                        }
                    }

                    NumberSelectionMenu(
                        itemType = "новой ${
                            if (selectedOpForReplacement?.itemType == "DUCK") "утки" else "яхты"
                        }",
                        occupiedNumbers = filteredOccupied,
                        isReplacementMode = true,
                        onNumbersSelected = { numbers ->
                            numbers.firstOrNull()?.let { newId ->
                                selectedOpForReplacement?.let { op ->
                                    viewModel.viewModelScope.launch {
                                        viewModel.replaceGroupItem(
                                            groupId,
                                            op.itemId,
                                            newId
                                        )
                                    }
                                }
                            }
                            showNumberSelection = false
                        },
                        onDismiss = { showNumberSelection = false }
                    )
                }
            }
        },
        confirmButton = {
            Column {
                // Кнопка продления
                Button(
                    onClick = onExtendRental,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA000)
                    )
                ) {
                    Text("Продлить всю группу на 20 мин")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопка сохранения
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Готово")
                }
            }
        }
    )
}

