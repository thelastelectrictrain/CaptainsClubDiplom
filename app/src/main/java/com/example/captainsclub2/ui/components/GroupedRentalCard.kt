package com.example.captainsclub2.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.captainsclub2.data.models.Operation
import kotlinx.coroutines.delay
//import androidx.compose.foundation.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.example.captainsclub2.utils.TimeUtils

@Composable
fun GroupedRentalCard(
    operations: List<Operation>,
    onAction: (String) -> Unit,
    onSettingsClick: () -> Unit,
    initialRemainingTime: Int
) {
//    val firstOperation = operations.first()
//    //var remainingTime by remember(firstOperation.id) { mutableIntStateOf(initialRemainingTime) }
//    val formattedTime = remember(remainingTime) {
//        String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)
//    }
//    val progressBarColor = when {
//        remainingTime <= 0 -> Color.Red
//        remainingTime < 120 -> Color.Yellow
//        else -> Color(0xFF0D47A1)
//    }
//    val progress = 1f - (remainingTime / 1380f)
//    val startTimeFormatted = firstOperation.startTime.take(5)
//    //val endTimeFormatted = TimeUtils.calculateEndTime(firstOperation.startTime, 23).take(5)
//
//    // Рассчитываем общее время аренды с учетом продлений
//    val totalDurationMinutes = 23 + (firstOperation.extensionsCount * 20)
//    val extendedRemainingTime = initialRemainingTime + (firstOperation.extensionsCount * 20 * 60)
//
//    var remainingTime by remember(firstOperation.id) {
//        mutableIntStateOf(extendedRemainingTime)
//    }
//    val formattedTotalDuration = "${totalDurationMinutes / 20 * 20} мин"
//
//    // Обновляем расчет времени окончания
//    val endTimeFormatted = TimeUtils.calculateEndTime(
//        operation.startTime,
//        23 + (operation.extensionsCount * 20)
//    ).take(5)
//
//    LaunchedEffect(firstOperation.id) {
//        while (remainingTime > 0) {
//            delay(1000)
//            remainingTime -= 1
//        }
//    }
//
//    val title = "${if (firstOperation.itemType == "DUCK") "Утки" else "Яхты"} ${
//        operations.joinToString(", ") { it.itemId.toString() }
//    }"

    val firstOperation = operations.first()

    // Рассчитываем общее время аренды с учетом продлений
    //val totalDurationMinutes = 23 + (firstOperation.extensionsCount * 20)

    val totalDurationMinutes = 23 + firstOperation.extensionsCount  * 20
    val formattedTotalDuration = "${(totalDurationMinutes / 20) * 20} мин"

    // Оставшееся время в секундах (изначальное + продления)
   // val totalDurationSeconds = (23 + firstOperation.extensionsCount * 20) * 60

    val totalDurationSeconds = remember(firstOperation.extensionsCount) {
        (23 + firstOperation.extensionsCount * 20) * 60
    }

    // Начальное оставшееся время + продления (в секундах)
    val extendedInitialTime = minOf(initialRemainingTime + (firstOperation.extensionsCount * 20 * 60), totalDurationSeconds)

    // Текущее оставшееся время (будет уменьшаться)
//    var remainingTime by remember(firstOperation.id) {
//        mutableIntStateOf(extendedInitialTime)
//    }

//    var remainingTime by remember(firstOperation.id) {
//        mutableIntStateOf(totalRemainingSeconds)
//    }

    //var remainingTime by remember(firstOperation.id) { mutableIntStateOf(extendedInitialTime) }

    val remainingSeconds = remember(firstOperation.id) {
        val elapsed = (System.currentTimeMillis() - firstOperation.startTimestamp) / 1000
        maxOf(0, totalDurationSeconds - elapsed.toInt())
    }
    //var remainingTime by remember { mutableIntStateOf(remainingSeconds) }

    var remainingTime by remember { mutableIntStateOf(0) }

    // Функция для пересчета оставшегося времени
    fun calculateRemainingTime(): Int {
        val elapsed = (System.currentTimeMillis() - firstOperation.startTimestamp) / 1000
        return maxOf(0, totalDurationSeconds - elapsed.toInt())
    }

    // Форматированное время для отображения
    val formattedTime = remember(remainingTime) {
        String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)
    }

    // Цвет прогресс-бара
    val progressBarColor = when {
        remainingTime <= 0 -> Color.Red
        remainingTime < 120 -> Color(0xFFFFA000)
        else -> Color(0xFF0D47A1)
    }

    // Прогресс (от 0 до 1)
    //val progress = 1f - (remainingTime.toFloat() / (23 * 60 + firstOperation.extensionsCount * 20 * 60).toFloat())

    //val progress = 1f - (remainingTime.toFloat() / (totalDurationMinutes * 60).toFloat())

    // Прогресс рассчитываем как потраченное время / общее время
    val progress = 1f - (remainingTime.toFloat() / totalDurationSeconds.toFloat())

    // Время начала и окончания аренды
    val startTimeFormatted = firstOperation.startTime.take(5)
    val endTimeFormatted = TimeUtils.calculateEndTime(
        firstOperation.startTime,
        23 + (firstOperation.extensionsCount * 20)
    ).take(5)

    // Таймер
//    LaunchedEffect(firstOperation.id) {
//        while (remainingTime > 0) {
//            delay(1000)
//            remainingTime -= 1
//        }
//    }

    // Инициализируем и обновляем таймер
    LaunchedEffect(firstOperation.extensionsCount, firstOperation.startTimestamp) {
        remainingTime = calculateRemainingTime()
        while (remainingTime > 0) {
            delay(1000)
            remainingTime = calculateRemainingTime() // Пересчитываем каждый раз
        }
    }

    val title = "${if (firstOperation.itemType == "DUCK") "Утки" else "Яхты"} ${
        operations.joinToString(", ") { it.itemId.toString() }
    }"

    // Добавляем состояние для подтверждения отмены
    var showCancelConfirmation by remember { mutableStateOf(false) }

    // Меню подтверждения отмены
    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text("Подтверждение отмены") },
            text = { Text("Вы уверены, что хотите отменить групповую аренду?") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmation = false
                        onAction("CANCEL")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCancelConfirmation = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                ) {
                    Text("Закрыть")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
//            // Кнопка отмены
//            IconButton(
//                onClick = { showCancelConfirmation = true },
//                modifier = Modifier
//                    .size(40.dp)
//                    .clip(CircleShape)
//                    .background(Color.Red)
//            ) {
//                Icon(Icons.Default.Close, contentDescription = "Отменить все", tint = Color.White)
//            }

            // Левый блок - кнопка отмены и настроек
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.weight(1f)
            ) {
                // Кнопка отмены
                IconButton(
                    onClick = { showCancelConfirmation = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Отменить все", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Кнопка настроек
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D47A1))
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Настройки",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }

            // Центральный блок
//            Column(
//                modifier = Modifier.weight(1f),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                val density = LocalDensity.current
//                Box(
//                    modifier = Modifier.fillMaxWidth(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    var textWidth by remember { mutableStateOf(0) }
//
//                    Text(
//                        text = title,
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier
//                            .padding(end = 28.dp)
//                            .onGloballyPositioned {
//                                textWidth = it.size.width
//                            }
//                    )
//
//                    IconButton(
//                        onClick = onSettingsClick,
//                        modifier = Modifier
//                            .size(20.dp)
//                            .align(Alignment.Center)
//                            .offset(x = with(density) {
//                                (textWidth / 2 + 8).toDp()
//                            })
//                    ) {
//                        Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = Color.Blue)
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(4.dp))
//
//                Text(text = "$startTimeFormatted - $endTimeFormatted", fontSize = 12.sp)
//                Spacer(modifier = Modifier.height(2.dp))
//                Text(text = formattedTime, fontSize = 14.sp, fontWeight = FontWeight.Medium)
//                Spacer(modifier = Modifier.height(2.dp))
//
//                LinearProgressIndicator(
//                    progress = progress,
//                    modifier = Modifier
//                        .fillMaxWidth(0.8f)
//                        .height(6.dp),
//                    color = progressBarColor
//                )
//            }

            // Центральный блок - теперь будет занимать 0 веса и центрироваться
            Box(
                modifier = Modifier.weight(4f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "$startTimeFormatted - $endTimeFormatted", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = formattedTime, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))

//                    LinearProgressIndicator(
//                        progress = progress,
//                        modifier = Modifier
//                            .fillMaxWidth(0.8f)
//                            .height(6.dp),
//                        color = progressBarColor
//                    )
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)) // Закругление углов прогресс-бара
                            .border(
                                width = 1.dp,
                                color = Color.LightGray.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(3.dp)
                            ),
                        color = progressBarColor,
                        trackColor = Color.Transparent // Прозрачный фон, чтобы был виден только контур
                    )
                }
            }

            // Кнопка завершения
//            IconButton(
//                onClick = { onAction("COMPLETE") },
//                modifier = Modifier
//                    .size(48.dp)
//                    .clip(CircleShape)
//                    .background(Color(0xFF388E3C))
//            ) {
//                Icon(Icons.Default.Check, contentDescription = "Завершить все", tint = Color.White)
//            }

            // Правый блок - кнопка завершения
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = { onAction("COMPLETE") },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF388E3C))
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Завершить все", tint = Color.White)
                }
            }
        }



        // Бейджи
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (operations.any { it.isCash }) {
                Badge(modifier = Modifier.padding(end = 2.dp), containerColor = Color(0xFF4CAF50)) {
                    Text("Наличка", fontSize = 10.sp, color = Color.White)
                }
            }
            if (operations.any { it.hasDiscount }) {
                Badge(containerColor = Color(0xFF2196F3)) {
                    Text("Скидка", fontSize = 10.sp, color = Color.White)
                }
            }

            // Бейдж с общим временем аренды
            if (firstOperation.extensionsCount > 0) {
                Badge(
                    containerColor = Color(0xFFFFA000),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(formattedTotalDuration)
                }
            }
        }

        // Добавляем комментарии для групповой аренды
            if (firstOperation.comment.isNotEmpty()) {
                Text(
                    text = "Комментарий: ${firstOperation.comment}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }


    }
    }
}

//// Вспомогательная функция для расчета ширины текста
//@Composable
//private fun calculateTextWidth(text: String, fontSize: TextUnit): Float {
//    val textLayoutResult = remember(text, fontSize) {
//        TextMeasurer().measure(
//            text = AnnotatedString(text),
//            style = TextStyle(fontSize = fontSize)
//        )
//    }
//    return textLayoutResult.size.width.toFloat()
//}