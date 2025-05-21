package com.example.captainsclub2.ui.screens


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import com.example.captainsclub2.ui.components.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captainsclub2.data.models.Operation
import com.example.captainsclub2.ui.components.FineSelectionDialog
import com.example.captainsclub2.ui.components.NumberSelectionMenu
import com.example.captainsclub2.ui.components.RentalCard
import com.example.captainsclub2.ui.components.GroupedRentalCard
import com.example.captainsclub2.ui.components.GroupedRentalSettingsDialog
import com.example.captainsclub2.ui.components.SouvenirSelectionDialog
import com.example.captainsclub2.ui.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.collectAsState
import androidx.compose.runtime.getValue
import com.example.captainsclub2.utils.LogTags.TAG_OPERATION
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


@Composable
fun RentalScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val operations by viewModel.operations.collectAsState()
    val matrosOnShift by viewModel.matrosOnShift.collectAsState()
    val isEnabled = matrosOnShift.isNotEmpty()
    val listState = rememberLazyListState()
   // var unseenCardsCount by rememberSaveable { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val rentalOperations by remember(operations) {
        derivedStateOf {
            operations.filter {
                it.type == "RENTAL" &&
                        it.status == "ACTIVE" &&
                        !it.isExtension // Исключаем операции продлений
            }.sortedBy { it.startTimestamp }
        }
    }

    // для замены номера
    var showNumberSelectionDialog by remember { mutableStateOf(false) }
    var selectedForReplacement by remember { mutableStateOf<Operation?>(null) }

    // Получаем занятые номера с учетом типа
    val occupiedNumbers by remember(operations, selectedForReplacement) {
        derivedStateOf {
            operations
                .filter {
                    it.type == "RENTAL" &&
                            it.status == "ACTIVE" &&
                            it.itemType == selectedForReplacement?.itemType
                }
                .map { it.itemId }
        }
    }

    // Состояние для хранения индекса последней просмотренной карточки
    val lastSeenIndex by viewModel.lastSeenRentalIndex.collectAsState()
    var unseenCardsCount by rememberSaveable { mutableStateOf(0) }
    var hasNewCards by rememberSaveable { mutableStateOf(false) }

    // Эффект для обработки новых карточек и видимости
    LaunchedEffect(rentalOperations.size, listState.isScrollInProgress) {
        if (rentalOperations.isEmpty()) return@LaunchedEffect

        val layoutInfo = listState.layoutInfo
        val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val totalItemsCount = layoutInfo.totalItemsCount - 1 // последний индекс

        // Проверяем, есть ли новые карточки после последнего просмотренного
        val newCardsExist = totalItemsCount > lastSeenIndex

        if (newCardsExist) {
            hasNewCards = true

            // Проверяем, видна ли последняя карточка
            val isLastItemVisible = lastVisibleIndex >= totalItemsCount

            if (!isLastItemVisible) {
                // Если есть новые карточки и они не видны - обновляем счетчик
                unseenCardsCount = totalItemsCount - lastSeenIndex
            } else if (hasNewCards) {
                // Если пользователь прокрутил до новых карточек - отмечаем как просмотренные
                viewModel.updateLastSeenIndex(totalItemsCount)
                unseenCardsCount = 0
                hasNewCards = false
            }
        }
    }

    // Эффект для сброса состояния при изменении списка операций
    LaunchedEffect(rentalOperations.size) {
        if (rentalOperations.size <= lastSeenIndex + 1) {
            unseenCardsCount = 0
            hasNewCards = false
            viewModel.updateLastSeenIndex(rentalOperations.size - 1)
        }
    }

    var showDuckMenu by remember { mutableStateOf(false) }
    var showYachtMenu by remember { mutableStateOf(false) }
    var showSouvenirDialog by remember { mutableStateOf(false) }
    var showFineDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf<Operation?>(null) }
    var showGroupSettings by remember { mutableStateOf<String?>(null) }

    val groupedOperations by remember(operations) {
        derivedStateOf {
            operations
                .filter { it.type == "RENTAL" && it.status == "ACTIVE" }
                .groupBy { it.groupId }
                .values
                .sortedBy { group -> group.minOf { it.startTimestamp } }
        }
    }

    //val groupedRentals by viewModel.groupedRentals.collectAsState()
//    val groupedRentals by remember(operations) {
//        derivedStateOf {
//            operations
//                .filter { it.type == "RENTAL" && it.status == "ACTIVE" }
//                .groupBy { if (it.groupId.isNotEmpty()) it.groupId else "single_${it.id}" } // Группируем по groupId или id
//        }
//    }
    val groupedRentals by remember(operations) {
        derivedStateOf {
            operations
                .filter { it.type == "RENTAL" && it.status == "ACTIVE" }
                .groupBy { op ->
                    when {
                        // Групповые аренды - по groupId
                        op.groupId.isNotEmpty() && !op.groupId.startsWith("ext_") -> op.groupId
                        // Одиночные аренды - по id (исключаем продления)
                        else -> "single_${op.id}"
                    }
                }
                .mapValues { (_, ops) ->
                    // Для групповых берем оригинальные операции
                    if (ops.any { it.groupId.isNotEmpty() && !it.groupId.startsWith("ext_") }) {
                        ops.filter { !it.groupId.startsWith("ext_") }
                    } else {
                        // Для одиночных берем только основную операцию
                        listOf(ops.first())
                    }
                }
        }
    }

    val occupiedDuckNumbers = rentalOperations
        .filter { it.itemType == "DUCK" }
        .map { it.itemId }

    val occupiedYachtNumbers = rentalOperations
        .filter { it.itemType == "YACHT" }
        .map { it.itemId }



    Box(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(1f/3f)
                .padding(end = 8.dp)
        ) {
            Button(
                onClick = { showDuckMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(56.dp),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) Color(0xFF0D47A1) else Color.Gray,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text("Утки", fontSize = 18.sp)
            }
            Button(
                onClick = { showYachtMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(56.dp),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) Color(0xFF0D47A1) else Color.Gray,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text("Яхты", fontSize = 18.sp)
            }
            Button(
                onClick = { showSouvenirDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(56.dp),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) Color(0xFF0D47A1) else Color.Gray,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text("Сувениры", fontSize = 18.sp)
            }
            Button(
                onClick = { showFineDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(56.dp),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) Color(0xFF0D47A1) else Color.Gray,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text("Штрафы", fontSize = 18.sp)
            }
        }

        Divider(
            color = Color(0xFF9E9E9E),
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                //.padding(end = 8.dp) // Добавляем отступ справа
        )

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = listState) {
                items(groupedRentals.values.toList().sortedBy { it.first().startTimestamp }) { operations ->
                    Box(modifier = Modifier.padding(start = 16.dp)) {
                    if (operations.size > 1) {
                        GroupedRentalCard(
                            operations = operations,
                            onAction = { action ->
                                viewModel.viewModelScope.launch {
                                    operations.forEach { op ->
                                        when (action) {
                                            "CANCEL" -> viewModel.cancelOperation(op.id)
                                            "COMPLETE" -> viewModel.completeOperation(op.id)
                                        }
                                    }
                                }
                            },
                            onSettingsClick = { showGroupSettings = operations.first().groupId },
                            initialRemainingTime = viewModel.calculateRemainingTime(operations.first())
                        )
                    } else {
                        RentalCard(
                            operation = operations.first(),
                            onAction = { action ->
                                viewModel.viewModelScope.launch {
                                    when (action) {
                                        "CANCEL" -> viewModel.cancelOperation(operations.first().id)
                                        "COMPLETE" -> viewModel.completeOperation(operations.first().id)
                                    }
                                }
                            },
                            onSettingsClick = { showSettingsDialog = operations.first() },
                            initialRemainingTime = viewModel.calculateRemainingTime(operations.first()),
                            viewModel = viewModel
                        )
                    }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Кнопка прокрутки (показываем только если есть непросмотренные карточки)
            if (unseenCardsCount > 0) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(rentalOperations.size - 1)
                            viewModel.updateLastSeenIndex(rentalOperations.size - 1)
                            unseenCardsCount = 0
                            hasNewCards = false
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = Color(0xFF0D47A1)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Badge(
                            modifier = Modifier.offset(y = (-8).dp),
                            containerColor = Color.Red
                        ) {
                            Text(unseenCardsCount.toString())
                        }
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Новые карточки"
                        )
                    }
                }
            }
        }
    }
    }



    // Обработчики меню
    if (showDuckMenu) {
        NumberSelectionMenu(
            itemType = "утки",
            occupiedNumbers = occupiedDuckNumbers,
            onNumbersSelected = { numbers ->
                viewModel.viewModelScope.launch {
                    // Создаем одну групповую аренду
                    viewModel.createGroupRental("RENTAL", "DUCK", numbers)
                    showDuckMenu = false
                }
            },
            onDismiss = { showDuckMenu = false }
        )
    }

    // Аналогично для яхт
    if (showYachtMenu) {
        NumberSelectionMenu(
            itemType = "яхты",
            occupiedNumbers = occupiedYachtNumbers,
            onNumbersSelected = { numbers ->
                viewModel.viewModelScope.launch {
                    viewModel.createGroupRental("RENTAL", "YACHT", numbers)
                    showYachtMenu = false
                }
            },
            onDismiss = { showYachtMenu = false }
        )
    }

    if (showSouvenirDialog) {
        SouvenirSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showSouvenirDialog = false }
        )
    }

    if (showFineDialog) {
        FineSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showFineDialog = false }
        )
    }

    // Обработчик групповых настроек
//    showGroupSettings?.let { groupId ->
//        val groupOperations = operations
//            .filter { it.groupId == groupId && it.status == "ACTIVE" }
//
//        GroupedRentalSettingsDialog(
//            operations = groupOperations,
//            onUpdate = { updatedOp ->
//                viewModel.viewModelScope.launch {
//                    viewModel.updateOperation(updatedOp)
//                }
//            },
//            onExtendRental = {
//                viewModel.viewModelScope.launch {
//                    // Продлеваем всю группу
//                    viewModel.extendRental(groupOperations.first(), true)
//                }
//            },
//            onDismiss = { showGroupSettings = null }
//        )
//    }

    showGroupSettings?.let { groupId ->
        val groupOperations = operations
            .filter { it.groupId == groupId && it.status == "ACTIVE" }

        GroupedRentalSettingsDialog(
            operations = groupOperations,
//            onUpdate = { updatedOp ->
//                viewModel.viewModelScope.launch {
//                    viewModel.updateOperation(updatedOp)
//                }
//            },
            onUpdate = { updatedOp ->
                viewModel.viewModelScope.launch {
                    viewModel.updateOperation(updatedOp)
                    viewModel.updateGroupExtensions(
                        updatedOp.groupId,
                        updatedOp.isCash,
                        updatedOp.hasDiscount
                    )
                }
            },
            onExtendRental = {
                viewModel.viewModelScope.launch {
                    viewModel.extendGroupRental(groupId)
                }
            },
            onDismiss = { showGroupSettings = null }
        )
    }

//    обработчик одиночных нстроек

    showSettingsDialog?.let { operation ->
//        var comment by remember { mutableStateOf(operation.comment) }
//        var isCash by remember { mutableStateOf(operation.isCash) }
//        var hasDiscount by remember { mutableStateOf(operation.hasDiscount) }
//        var extensions by remember { mutableStateOf(operation.extensionsCount) }

        // Получаем актуальные данные операции из ViewModel
        val currentOp by viewModel.operations
            .map { ops -> ops.firstOrNull { it.id == operation.id } ?: operation }
            .collectAsState(initial = operation)

        // Локальное состояние должно синхронизироваться с текущей операцией
        var isCash by remember { mutableStateOf(currentOp.isCash) }
        var hasDiscount by remember { mutableStateOf(currentOp.hasDiscount) }
        var comment by remember { mutableStateOf(currentOp.comment) }
        var extensions by remember { mutableStateOf(currentOp.extensionsCount) }

        var saveJob by remember { mutableStateOf<Job?>(null) }

        LaunchedEffect(currentOp) {
            // Синхронизируем локальное состояние при изменении операции
            isCash = currentOp.isCash
            hasDiscount = currentOp.hasDiscount
            comment = currentOp.comment
            extensions = currentOp.extensionsCount
        }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = null },
            title = { Text("Настройки аренды") },
            text = {
                Column {
                    //Text("${if (operation.itemType == "DUCK") "Утка" else "Яхта"} ${operation.itemId}")
                    Text("${if (currentOp.itemType == "DUCK") "Утка" else "Яхта"} ${currentOp.itemId}")
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
                                    currentOp.copy(
                                        comment = newComment
                                    )
                                )
                            }
                        },
                        label = { Text("Комментарий") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Checkbox(
//                            checked = isCash,
//                            onCheckedChange = { isCash = it },
//                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0D47A1))
//                        )
//                        Text("Наличные", modifier = Modifier.padding(start = 8.dp))
                        Checkbox(
                            checked = isCash,
//                            onCheckedChange = { isChecked ->
//                                isCash = isChecked
//                                viewModel.viewModelScope.launch {
//                                    viewModel.updateOperation(operation.copy(isCash = isCash))
//                                    viewModel.updateRentalExtensions(operation.id, isCash, hasDiscount)
//                                }
//                            },
                            onCheckedChange = { checked ->
                                isCash = checked
                                viewModel.viewModelScope.launch {
                                    viewModel.updateOperation(
                                        currentOp.copy(
                                            isCash = checked,
                                            hasDiscount = hasDiscount
                                        )
                                    )
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0D47A1))
                        )
                        Text("Наличные", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Checkbox(
//                            checked = hasDiscount,
//                            onCheckedChange = { hasDiscount = it },
//                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0D47A1))
//                        )
//                        Text("Скидка", modifier = Modifier.padding(start = 8.dp))
                        Checkbox(
                            checked = hasDiscount,
//                            onCheckedChange = { isChecked ->
//                                hasDiscount = isChecked
//                                Log.d(TAG_OPERATION, "Discount checkbox changed: $isChecked")
//                                viewModel.viewModelScope.launch {
//                                    // Обновляем цену с учетом нового состояния скидки
//                                    val newPrice = if (isChecked) 175.0 else 350.0
//                                    viewModel.updateOperation(
//                                        operation.copy(
//                                            hasDiscount = isChecked,
//                                            price = newPrice
//                                        )
//                                    )
//                                    // Обновляем все продления
//                                    viewModel.updateRentalExtensions(
//                                        operation.id,
//                                        operation.isCash,
//                                        isChecked
//                                    )
//                                }
//                            },
                            onCheckedChange = { checked ->
                                hasDiscount = checked
                                viewModel.viewModelScope.launch {
                                    viewModel.updateOperation(
                                        currentOp.copy(
                                            isCash = isCash,
                                            hasDiscount = checked,
                                            price = if (checked) 175.0 else 350.0
                                        )
                                    )
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0D47A1))
                        )
                        Text("Скидка", modifier = Modifier.padding(start = 8.dp))
                    }

                    // Кнопка продления для одиночной аренды
                    Button(
//                        onClick = {
//                            viewModel.viewModelScope.launch {
//                                viewModel.extendSingleRental(operation)
//                            }
//                        },
//                        onClick = {
//                            viewModel.viewModelScope.launch {
//                                viewModel.extendSingleRental(operation.copy(isCash = isCash, hasDiscount = hasDiscount))
//                                // Обновляем операцию с текущими значениями чекбоксов
//                                viewModel.updateOperation(operation.copy(isCash = isCash, hasDiscount = hasDiscount))
//                                extensions = operation.extensionsCount + 1
//                            }
//                        },
                        onClick = {
                            viewModel.viewModelScope.launch {
                                viewModel.extendSingleRental(currentOp)
                                extensions = currentOp.extensionsCount + 1
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Продлить аренду")
                    }

                    // Бейдж с количеством продлений
                    if (extensions > 0) {
                        Badge(
                            containerColor = Color(0xFFFFA000),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("+${extensions * 20} мин")
                        }
                    }

                    Button(
                        onClick = {
                            // Показываем меню выбора нового номера
//                            val occupiedNumbers = operations
//                                .filter { it.type == "RENTAL" && it.status == "ACTIVE" }
//                                .map { it.itemId }
//
//                            // Исключаем текущий номер из занятых, так как мы его заменяем
//                            val filteredOccupied = occupiedNumbers.filter { it != operation.itemId }

                            showNumberSelectionDialog = true
                            selectedForReplacement = operation
                        },
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFA000)
                        )
                    ) {
                        Text("Заменить номер")
                    }

                    if (showNumberSelectionDialog && selectedForReplacement != null) {
                        NumberSelectionMenu(
                            itemType = if (selectedForReplacement?.itemType == "DUCK") "новой утки" else "новой яхты",
//                            occupiedNumbers = operations
//                                .filter {
//                                    it.type == "RENTAL" &&
//                                            it.status == "ACTIVE" &&
//                                            selectedForReplacement?.let { sf -> it.itemId != sf.itemId } ?: true
//                                }
//                                .map { it.itemId },
                            occupiedNumbers = occupiedNumbers.filter { it != selectedForReplacement?.itemId },
                            isReplacementMode = true,
                            onNumbersSelected = { numbers ->
                                numbers.firstOrNull()?.let { newId ->
                                    viewModel.viewModelScope.launch {  // Оберните в coroutine
                                        viewModel.replaceRentalItem(selectedForReplacement?.id ?: return@launch, newId)
                                    }
                                }
                                showNumberSelectionDialog = false
                            },
                            onDismiss = { showNumberSelectionDialog = false }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            showSettingsDialog = null // Просто закрываем диалог
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                    ) {
                        Text("Готово")
                    }
                
            }
        )
    }
}

private fun calculateTotalPrice(op: Operation): Double {
    return if (op.hasDiscount) 175.0 else 350.0 // Базовая цена без учета продлений
    // Продления учитываются отдельно через RentalExtension
}

