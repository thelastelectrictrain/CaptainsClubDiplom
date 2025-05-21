package com.example.captainsclub2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captainsclub2.data.models.Operation
import com.example.captainsclub2.data.models.RentalExtension
import com.example.captainsclub2.ui.viewmodels.MainViewModel
import com.example.captainsclub2.utils.TimeUtils
//import androidx.compose.runtime.MutableStateMap

@Composable
fun ReportScreen(viewModel: MainViewModel = hiltViewModel()) {
    val operations by viewModel.operations.collectAsState()
    val extensions by viewModel.rentalExtensions.collectAsState()
    //val scrollState = rememberLazyListState()

    // Создаем объединенный список операций и продлений
//    val displayItems = remember(operations, extensions) {
//        val items = mutableListOf<DisplayItem>()
//
//        operations.forEach { op ->
//            when (op.type) {
//                "MATROS_ACTION" -> items.add(DisplayItem.MatrosActionItem(op))
//                else -> items.add(DisplayItem.OperationItem(op))
//            }
//        }
//
//        extensions.forEach { ext ->
//            operations.firstOrNull { it.id == ext.originalOperationId }?.let { originalOp ->
//                items.add(DisplayItem.ExtensionItem(ext, originalOp))
//            }
//        }
//
//        items.sortedBy {
//            when (it) {
//                is DisplayItem.OperationItem -> it.operation.startTimestamp
//                is DisplayItem.ExtensionItem -> it.extension.timestamp
//                is DisplayItem.MatrosActionItem -> it.operation.startTimestamp
//            }
//        }
//    }

    val displayItems = remember(operations, extensions) {
        val items = mutableListOf<DisplayItem>()

        operations.forEach { op ->
            when (op.type) {
                "MATROS_ACTION" -> items.add(DisplayItem.MatrosActionItem(op))
                else -> items.add(DisplayItem.OperationItem(op))
            }
        }

        extensions.forEach { ext ->
            operations.firstOrNull { it.id == ext.originalOperationId }?.let { originalOp ->
                items.add(DisplayItem.ExtensionItem(ext, originalOp))
            }
        }

        items.sortedBy {
            when (it) {
                is DisplayItem.OperationItem -> it.operation.startTimestamp
                is DisplayItem.ExtensionItem -> it.extension.timestamp
                is DisplayItem.MatrosActionItem -> it.operation.startTimestamp
            }
        }
    }

    // Нумерация для завершенных аренд и продлений
    val rentalNumbers = remember(displayItems) {
        displayItems
            .filter {
                when (it) {
                    is DisplayItem.OperationItem ->
                        it.operation.type == "RENTAL" && it.operation.status == "COMPLETED"
                    is DisplayItem.ExtensionItem ->
                        it.extension.status == "COMPLETED"
                    is DisplayItem.MatrosActionItem ->
                        false // Исключаем действия матросов из нумерации
                }
            }
            .mapIndexed { index, item ->
                when (item) {
                    is DisplayItem.OperationItem -> item.operation.id to (index + 1)
                    is DisplayItem.ExtensionItem -> item.extension.id to (index + 1)
                    // Для MatrosActionItem мы не должны попадать сюда благодаря фильтру выше
                    else -> throw IllegalStateException("Unexpected item type")
                }
            }
            .toMap()
    }

    // Статистика
    val stats = remember(displayItems) {
        // 1. Фильтруем только операции аренд, продлений, продаж и штрафов (исключаем действия матросов)
        val relevantItems = displayItems.filter {
            when (it) {
                is DisplayItem.OperationItem -> it.operation.type != "MATROS_ACTION" && it.operation.status == "COMPLETED"
                is DisplayItem.ExtensionItem -> it.extension.status == "COMPLETED"
                is DisplayItem.MatrosActionItem -> false
            }
        }

        // 2. Подсчет аренд (основных + продлений)
        val (totalRentals, duckRentals, yachtRentals) = relevantItems
            .filter {
                when (it) {
                    is DisplayItem.OperationItem -> it.operation.type == "RENTAL"
                    is DisplayItem.ExtensionItem -> true
                    else -> false
                }
            }
            .fold(Triple(0, 0, 0)) { (total, duck, yacht), item ->
                when (item) {
                    is DisplayItem.OperationItem -> Triple(
                        total + 1,
                        duck + if (item.operation.itemType == "DUCK") 1 else 0,
                        yacht + if (item.operation.itemType == "YACHT") 1 else 0
                    )
                    is DisplayItem.ExtensionItem -> Triple(
                        total + 1,
                        duck + if (item.originalOperation.itemType == "DUCK") 1 else 0,
                        yacht + if (item.originalOperation.itemType == "YACHT") 1 else 0
                    )
                    else -> Triple(total, duck, yacht)
                }
            }

        // 3. Подсчет сувениров по типам (только OperationItem типа SALE)
        val souvenirs = relevantItems
            .filterIsInstance<DisplayItem.OperationItem>()
            .filter { it.operation.type == "SALE" }
            .groupBy { it.operation.itemType }
            .mapValues { (_, ops) -> ops.size }

        // 4. Общее количество сувениров
        val totalSouvenirs = souvenirs.values.sum()

        // 5. Подсчет штрафов по типам (только OperationItem типа FINE)
        val fines = relevantItems
            .filterIsInstance<DisplayItem.OperationItem>()
            .filter { it.operation.type == "FINE" }
            .groupBy { it.operation.itemType }
            .mapValues { (_, ops) -> ops.size }

        // 6. Общее количество штрафов
        val totalFines = fines.values.sum()

        // 7. Возвращаем мапу со всей статистикой
        mapOf(
            "totalRentals" to totalRentals,
            "duckRentals" to duckRentals,
            "yachtRentals" to yachtRentals,
            "totalSouvenirs" to totalSouvenirs,
            "souvenirs" to souvenirs,
            "totalFines" to totalFines,
            "fines" to fines
        )
    }


    // Рассчитываем начальный индекс
    val initialIndex = remember(displayItems.size) {
        if (displayItems.isEmpty()) 0 else displayItems.size - 1
    }

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex
    )

    // Автоматическая прокрутка ТОЛЬКО при добавлении новых элементов
    var previousItemsSize by remember { mutableStateOf(displayItems.size) }

    LaunchedEffect(displayItems.size) {
        if (displayItems.size > previousItemsSize) {
            scrollState.animateScrollToItem(displayItems.size - 1)
            previousItemsSize = displayItems.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Отображение статистики
        Text(text = "Отчет", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
        ) {
            // Аренды
            Text(
                text = "Всего сдач: ${stats["totalRentals"]}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Уток: ${stats["duckRentals"]}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Яхт: ${stats["yachtRentals"]}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Сувениры
            Text(
                text = "Сувениры: ${stats["totalSouvenirs"]}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Штрафы
            Text(
                text = "Штрафы: ${stats["totalFines"]}",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider()
        }

        // Список операций и продлений
        LazyColumn(state = scrollState) {
            items(displayItems) { item ->
                when (item) {
                    is DisplayItem.OperationItem -> {
                        if (item.operation.type != "MATROS_ACTION") {
                            OperationCard(
                                operation = item.operation,
                                rentalNumbers = rentalNumbers,
                                viewModel = viewModel
                            )
                        }
                    }
                    is DisplayItem.ExtensionItem ->
                        OperationCard(
                            extension = item.extension,
                            originalOperation = item.originalOperation,
                            rentalNumbers = rentalNumbers,
                            viewModel = viewModel
                        )
                    is DisplayItem.MatrosActionItem ->
                        MatrosActionItem(
                            operation = item.operation,
                            viewModel = viewModel
                        )
                }
            }
        }
    }
}

// Вспомогательный sealed class для объединенного списка
sealed class DisplayItem {
    data class OperationItem(val operation: Operation) : DisplayItem()
    data class ExtensionItem(val extension: RentalExtension, val originalOperation: Operation) : DisplayItem()
    data class MatrosActionItem(val operation: Operation) : DisplayItem() // Новый тип для действий матросов
}

@Composable
fun MatrosActionItem(
    operation: Operation,
    viewModel: MainViewModel
) {
    val matrosName = viewModel.getMatrosName(operation.itemId)
    val actionText = when (operation.itemType) {
        "JOIN_SHIFT" -> "приступает к смене"
        "LEAVE_SHIFT" -> "покидает смену"
        else -> "выполняет действие"
    }

    Text(
        text = "${operation.startTime.take(5)} - $matrosName $actionText",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

//    Divider(modifier = Modifier.padding(vertical = 4.dp))
}

//@Composable
//fun OperationCard(
//    operation: Operation,
//    rentalNumbers: Map<Int, Int> // Map<"DUCK"/"YACHT" to Map<itemId, номер>>
//) {
//    // Получаем номер только для завершенных аренд
//    val counter = if (operation.type == "RENTAL") {
//        when (operation.status) {
//            "COMPLETED" -> rentalNumbers[operation.id]
//            else -> null
//        }
//    } else null
//
//    val (title, timeText, statusInfo) = when (operation.type) {
//        "RENTAL" -> {
//            val itemName = if (operation.itemType == "DUCK") "Утка" else "Яхта"
//            val statusText = when (operation.status) {
//                "COMPLETED" -> "Выполнено"
//                "CANCELED" -> "Отменено"
//                else -> "В процессе"
//            }
//            val statusColor = when (operation.status) {
//                "COMPLETED" -> Color(0xFF388E3C)
//                "CANCELED" -> Color.Red
//                else -> MaterialTheme.colorScheme.onSurface
//            }
//
//            Triple(
//                "${counter?.let { "$it. " } ?: ""}$itemName ${operation.itemId}",
//                "${TimeUtils.formatTimeForDisplay(operation.startTime)} - " +
//                        "${TimeUtils.calculateEndTime(operation.startTime, 23).take(5)}",
//                statusText to statusColor
//            )
//        }
//        "SALE", "FINE" -> {
//            val title = when (operation.itemType) {
//                "HAT" -> "Шапка"
//                "KEYCHAIN" -> "Брелок"
//                "ROPE" -> "Верёвка"
//                "DUCK_SALE" -> "Утка (продажа)"
//                "STATUETTE" -> "Статуэтка"
//                "FINE_DUCK" -> "Штраф утка"
//                "FINE_YACHT" -> "Штраф яхта"
//                else -> operation.itemType
//            }
//
//            Triple(
//                title,
//                TimeUtils.formatTimeForDisplay(operation.startTime),
//                "Выполнено" to Color(0xFF388E3C)
//            )
//        }
//        else -> Triple("", "", "" to Color.Transparent)
//    }
//
//    // Остальная часть компонента остается без изменений
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.2f))
//    ) {
//        Box(modifier = Modifier.padding(16.dp)) {
//            // Пометки в правом верхнем углу
//            Row(
//                modifier = Modifier
//                    .align(Alignment.TopEnd)
//                    .padding(bottom = 8.dp),
//                horizontalArrangement = Arrangement.End
//            ) {
//                if (operation.isCash) {
//                    Badge(
//                        containerColor = Color(0xFF388E3C),
//                        modifier = Modifier.padding(start = 4.dp)
//                    ) {
//                        Text("Наличка", color = Color.White)
//                    }
//                }
//                if (operation.hasDiscount) {
//                    Badge(
//                        containerColor = Color.Blue,
//                        modifier = Modifier.padding(start = 4.dp)
//                    ) {
//                        Text("Скидка", color = Color.White)
//                    }
//                }
//            }
//
//            Column(modifier = Modifier.fillMaxWidth()) {
//                Text(
//                    text = title,
//                    style = MaterialTheme.typography.titleMedium
//                )
//
//                Spacer(modifier = Modifier.height(4.dp))
//
//                Text(
//                    text = timeText,
//                    style = MaterialTheme.typography.bodySmall
//                )
//
//                Spacer(modifier = Modifier.height(4.dp))
//
//                Text(
//                    text = statusInfo.first,
//                    color = statusInfo.second,
//                    style = MaterialTheme.typography.bodyMedium
//                )
//            }
//        }
//    }
//}

@Composable
fun OperationCard(
    operation: Operation? = null,
    extension: RentalExtension? = null,
    originalOperation: Operation? = null,
    rentalNumbers: Map<Int, Int>,
    viewModel: MainViewModel
) {
    // Проверяем, что у нас есть либо operation, либо extension с originalOperation
    require(operation != null || (extension != null && originalOperation != null)) {
        "Must provide either operation or extension with originalOperation"
    }

    val isExtension = extension != null
    val currentOperation = operation ?: run {
        // Создаем временную операцию для отображения на основе продления
        Operation(
            id = extension!!.id,
            type = "RENTAL_EXTENSION",
            itemType = originalOperation!!.itemType,
            itemId = originalOperation.itemId,
            shiftId = originalOperation.shiftId,
            startTime = TimeUtils.formatTimeLong(extension.timestamp),
            startTimestamp = extension.timestamp,
            status = extension.status,
            isCash = extension.isCash,
            hasDiscount = extension.hasDiscount,
            price = extension.price,
            isExtension = true,
            //originalOperationId = extension.originalOperationId
        )
    }



    // Остальная часть компонента остается практически без изменений
    val counter = when {
        (currentOperation.type == "RENTAL" || isExtension) &&
                currentOperation.status == "COMPLETED" -> rentalNumbers[currentOperation.id]
        else -> null
    }

    // Формируем данные для отображения
    val (title, timeText, statusInfo) = when {
        currentOperation.type == "RENTAL" || isExtension -> {
            val itemName = when (currentOperation.itemType) {
                "DUCK" -> "Утка"
                "YACHT" -> "Яхта"
                else -> currentOperation.itemType
            }

            val statusText = when (currentOperation.status) {
                "COMPLETED" -> "Выполнено"
                "CANCELED" -> "Отменено"
                else -> "В процессе"
            }

            val statusColor = when (currentOperation.status) {
                "COMPLETED" -> Color(0xFF388E3C)
                "CANCELED" -> Color.Red
                else -> MaterialTheme.colorScheme.onSurface
            }

            val operationTitle = buildString {
                counter?.let { append("$it. ") }
                append(itemName)
                append(" ")
                append(currentOperation.itemId)
                if (isExtension) append(" (продление)")
            }

            Triple(
                operationTitle,
                "${TimeUtils.formatTimeForDisplay(currentOperation.startTime)} - " +
                        "${TimeUtils.calculateEndTime(currentOperation.startTime, 23).take(5)}",
                statusText to statusColor
            )
        }

        // Остальные случаи (продажи, штрафы)
        currentOperation.type == "SALE" -> {
            val itemName = when (currentOperation.itemType) {
                "HAT" -> "Шапка"
                "KEYCHAIN" -> "Брелок"
                "ROPE" -> "Верёвка"
                "DUCK_SALE" -> "Утка (продажа)"
                "STATUETTE" -> "Статуэтка"
                else -> currentOperation.itemType
            }
            Triple(itemName, currentOperation.startTime, "Выполнено" to Color(0xFF388E3C))
        }

        currentOperation.type == "FINE" -> {
            val itemName = when (currentOperation.itemType) {
                "FINE_DUCK" -> "Штраф утка"
                "FINE_YACHT" -> "Штраф яхта"
                else -> "Штраф"
            }
            Triple(itemName, currentOperation.startTime, "Выполнено" to Color(0xFF388E3C))
        }

        else -> Triple("", "", "" to Color.Transparent)
    }

    // Остальная часть компонента остается без изменений
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            // Пометки в правом верхнем углу
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (currentOperation.isCash) {
                    Badge(
                        containerColor = Color(0xFF388E3C),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("Наличка", color = Color.White)
                    }
                }
                if (currentOperation.hasDiscount) {
                    Badge(
                        containerColor = Color.Blue,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("Скидка", color = Color.White)
                    }
                }
                if (isExtension) {
                    Badge(
                        containerColor = Color(0xFFFFA000),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Продление",
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = statusInfo.first,
                    color = statusInfo.second,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun MatrosActionText(operation: Operation, viewModel: MainViewModel) {
    val matrosName = viewModel.getMatrosName(operation.itemId)
    val actionText = when (operation.itemType) {
        "JOIN_SHIFT" -> "приступает к смене"
        else -> "покидает смену"
    }

    Text(
        text = "${operation.startTime.take(5)} - $matrosName $actionText",
        modifier = Modifier.padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium
    )
}











