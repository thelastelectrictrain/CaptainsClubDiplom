package com.example.captainsclub2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captainsclub2.data.models.Matros
import com.example.captainsclub2.repository.ShiftRepository
import com.example.captainsclub2.ui.viewmodels.MainViewModel
//import com.example.captainsclub2.ui.viewmodels.MatrosState

@Composable
fun MatrosScreen(viewModel: MainViewModel = hiltViewModel()) {
    val permanentMatros by viewModel.permanentMatros.collectAsState()
    val matrosOnShift by viewModel.matrosOnShift.collectAsState()
    var showAddMatrosDialog by remember { mutableStateOf(false) }
    var newMatrosName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Диалог для добавления нового матроса
    if (showAddMatrosDialog) {
        AlertDialog(
            onDismissRequest = { showAddMatrosDialog = false },
            title = { Text("Добавить нового матроса") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newMatrosName,
                        onValueChange = { newMatrosName = it },
                        label = { Text("Имя матроса") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMatrosName.isNotBlank()) {
                            viewModel.createMatros(newMatrosName)
                            newMatrosName = ""
                            showAddMatrosDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMatrosDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Левая колонка - Команда
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.Top)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Команда",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(
                            onClick = { showAddMatrosDialog = true },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF0D47A1),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить матроса"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(permanentMatros.filter { matros ->
                            !matrosOnShift.any { it.id == matros.id }
                        }) { matros ->
                            MatrosCard(
                                matros = matros,
                                isOnShift = false,
                                onAction = { action ->
                                    when (action) {
                                        "JOIN" -> viewModel.addMatrosToShift(matros.id)
                                        "REMOVE" -> viewModel.deleteMatros(matros.id)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Правая колонка - На смене
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.Top)
                ) {
                    Text(
                        "На смене",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(matrosOnShift) { matros ->
                            MatrosCard(
                                matros = matros,
                                isOnShift = true,
                                onAction = { action ->
                                    if (action == "LEAVE") {
                                        viewModel.removeMatrosFromShift(matros.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatrosCard(
    matros: Matros,
    isOnShift: Boolean,
    onAction: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
        ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    matros.name,
                    modifier = Modifier
                        .weight(1f),
                    style = MaterialTheme.typography.bodyLarge)
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.MoreVert, "Действия")
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (!isOnShift) {
                    DropdownMenuItem(
                        text = { Text("Присоединиться к смене") },
                        onClick = {
                            onAction("JOIN")
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить из команды") },
                        onClick = {
                            onAction("REMOVE")
                            expanded = false
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Покинуть смену") },
                        onClick = {
                            onAction("LEAVE")
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}