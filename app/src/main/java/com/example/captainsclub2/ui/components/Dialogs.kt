package com.example.captainsclub2.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.example.captainsclub2.ui.viewmodels.MainViewModel
import com.example.captainsclub2.data.models.Souvenir
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel

// Dialogs.kt
@Composable
fun SouvenirSelectionDialog(
    viewModel: MainViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val availableSouvenirs by viewModel.availableSouvenirs.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var isDeleteMode by remember { mutableStateOf(false) }
    var showEditMenu by remember { mutableStateOf(false) }
    var isCash by remember { mutableStateOf(false) }
    var hasDiscount by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите сувенир") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Список сувениров
                availableSouvenirs.forEach { souvenir ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (!isDeleteMode) {
                                    coroutineScope.launch {
                                        viewModel.createSouvenirSale(
                                            itemName = souvenir.name,
                                            isCash = isCash,
                                            hasDiscount = hasDiscount
                                        )
                                    }
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                            enabled = !isDeleteMode
                        ) {
                            Text("${souvenir.name} - ${souvenir.price} ₽")
                        }

                        if (isDeleteMode) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.deleteSouvenir(souvenir.name)
                                    }
                                },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Удалить",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Чекбоксы оплаты
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Checkbox(
                        checked = isCash,
                        onCheckedChange = { isCash = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0D47A1))
                    )
                    Text("Наличка", modifier = Modifier.padding(start = 8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Checkbox(
                        checked = hasDiscount,
                        onCheckedChange = { hasDiscount = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0D47A1))
                    )
                    Text("Скидка", modifier = Modifier.padding(start = 8.dp))
                }

                // Кнопка редактирования с выпадающим меню
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { showEditMenu = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать"
                        )
                    }

                    DropdownMenu(
                        expanded = showEditMenu,
                        onDismissRequest = { showEditMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Добавить сувенир") },
                            onClick = {
                                showEditMenu = false
                                showAddDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить сувенир") },
                            onClick = {
                                showEditMenu = false
                                isDeleteMode = true
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isDeleteMode) {
                Button(
                    onClick = { isDeleteMode = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                ) {
                    Text("Готово")
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                    ) {
                    Text("Закрыть")
                }
            }
        }
    )

    // Диалог добавления нового сувенира
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Добавить сувенир") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Цена") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && price.isNotBlank()) {
                            viewModel.addNewSouvenirType(name, price.toDoubleOrNull() ?: 0.0)
                            showAddDialog = false
                        }
                    },
                    enabled = name.isNotBlank() && price.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF0D47A1) // Синий цвет текста
                    )
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun FineSelectionDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val availableFines by viewModel.availableFines.collectAsState()
    var isCash by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Оформить штраф") },
        text = {
            Column {
                availableFines.forEach { fine ->
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.createFine(
                                    fineType = fine,
                                    isCash = isCash)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                    ) {
                        Text(fine)
                    }
                }

                // Switch только для налички
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Checkbox(
                        checked = isCash,
                        onCheckedChange = { isCash = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0D47A1))
                    )
                    Text("Наличные", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
            ) {
                Text("Закрыть")
            }
        }
    )
}