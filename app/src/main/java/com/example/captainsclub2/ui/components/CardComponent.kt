package com.example.captainsclub2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.captainsclub2.data.models.Operation
import com.example.captainsclub2.ui.viewmodels.MainViewModel
import com.example.captainsclub2.utils.TimeUtils
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun RentalCard(
    operation: Operation,
    onAction: (String) -> Unit,
    onSettingsClick: () -> Unit,
    initialRemainingTime: Int,
    viewModel: MainViewModel // Добавляем viewModel для вызова extendRental
) {
    if (operation.type != "RENTAL" || (operation.itemType != "DUCK" && operation.itemType != "YACHT")) {
        return
    }

    //var remainingTime by remember(operation.id) { mutableIntStateOf(initialRemainingTime) }


    val totalDurationMinutes = 23 + (operation.extensionsCount * 20)

    //val totalDurationSeconds = totalDurationMinutes * 60

    val totalDurationSeconds = remember(operation.extensionsCount) {
        (23 + operation.extensionsCount * 20) * 60
    }


    val extendedInitialTime = minOf(initialRemainingTime + (operation.extensionsCount * 20 * 60), totalDurationSeconds)
//    var remainingTime by remember(operation.id) { mutableIntStateOf(extendedInitialTime) }
    val remainingSeconds = remember(operation.id) {
        val elapsed = (System.currentTimeMillis() - operation.startTimestamp) / 1000
        maxOf(0, totalDurationSeconds - elapsed.toInt())
    }


    //var remainingTime by remember { mutableIntStateOf(remainingSeconds) }
    var remainingTime by remember { mutableIntStateOf(0) }

    //val totalDurationMinutes = 23 + (operations.sumOf { it.extensionsCount } * 20)
//    val extendedInitialTime = initialRemainingTime + (operation.extensionsCount * 20 * 60)
//    var remainingTime by remember(operation.id) { mutableIntStateOf(extendedInitialTime) }


    // Функция для пересчета оставшегося времени
    fun calculateRemainingTime(): Int {
        val elapsed = (System.currentTimeMillis() - operation.startTimestamp) / 1000
        return maxOf(0, totalDurationSeconds - elapsed.toInt())
    }


    val formattedTime = TimeUtils.formatTime(remainingTime)
    val startTimeFormatted = operation.startTime.take(5)
    //val endTimeFormatted = TimeUtils.calculateEndTime(operation.startTime, 23).take(5)

    // Рассчитываем общее время аренды с учетом продлений
    //val totalDurationMinutes = 23 + (operation.extensionsCount * 20)
    val formattedTotalDuration = "${(totalDurationMinutes / 20) * 20} мин"

    // Рассчитываем оставшееся время с учетом продлений
    val extendedRemainingTime = initialRemainingTime + (operation.extensionsCount * 20 * 60)
//    var remainingTime by remember(operation.id) {
//        mutableIntStateOf(extendedRemainingTime)
//    }

    // Обновляем расчет времени окончания
    // Заменить на:
    val endTimeFormatted = TimeUtils.calculateEndTime(
        operation.startTime,
        23 + (operation.extensionsCount * 20)
    ).take(5)

    // Обновляем расчет времени окончания
//    val endTimeFormatted = TimeUtils.calculateEndTime(
//        operation.startTime,
//        totalDurationMinutes
//    ).take(5)

    val progressBarColor = when {
        remainingTime <= 0 -> Color.Red
        remainingTime < 120 -> Color(0xFFFFA000)
        else -> Color(0xFF0D47A1)
    }
    //val progress = 1f - (remainingTime / 1380f)
    // Прогресс рассчитываем как потраченное время / общее время
    val progress = 1f - (remainingTime.toFloat() / totalDurationSeconds.toFloat())

//    if (!LocalInspectionMode.current) {
//        LaunchedEffect(operation.id, operation.status) {
//            remainingTime = calculateRemainingTime()
//            if (operation.status == "ACTIVE") {
//                while (remainingTime > 0) {
//                    delay(1000)
//                    //remainingTime--
//                    remainingTime = calculateRemainingTime() // Пересчитываем каждый раз
//                }
//            }
//        }
//    }

    // Инициализируем и обновляем таймер
    LaunchedEffect(operation.extensionsCount, operation.startTimestamp) {
        remainingTime = calculateRemainingTime()
        while (remainingTime > 0) {
            delay(1000)
            remainingTime = calculateRemainingTime() // Пересчитываем каждый раз
        }
    }


    var showCancelConfirmation by remember { mutableStateOf(false) }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text("Подтверждение отмены") },
            text = { Text("Вы уверены, что хотите отменить аренду?") },
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
        Row( // Основной контейнер теперь Row
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // Левый блок - кнопка отмены и настроек
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.weight(1f)
            ) {
                // 1. Кнопка "Отменить" (слева)
                IconButton(
                    onClick = { showCancelConfirmation = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Отменить",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }


                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
//                    .size(20.dp)
//                    .align(Alignment.Center) // Выравниваем по центру
////                            .offset(x = with(LocalDensity.current) {
////                                // Рассчитываем смещение в зависимости от длины текста
////                                val textWidth = when (operation.itemType) {
////                                    "DUCK" -> "Утка ${operation.itemId}".length * 15
////                                    "YACHT" -> "Яхта ${operation.itemId}".length * 15
////                                    else -> 0
////                                }.toDp()
////                                textWidth / 2 + 12.dp // Подбираем оптимальное смещение
////                            })
//                    .offset(x = 40.dp)
//                    .padding(start=20.dp)
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

            // 2. Центральный блок с текстом и временем (центр)
            Box(
                modifier = Modifier
                    .weight(4f),
                //.padding(horizontal = 8.dp),
                //horizontalAlignment = Alignment.CenterHorizontally
                contentAlignment = Alignment.Center
            ) {
                // Строка с названием и иконкой настроек
                Column(
                    //modifier = Modifier.fillMaxWidth()
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (operation.itemType) {
                            "DUCK" -> "Утка ${operation.itemId}"
                            "YACHT" -> "Яхта ${operation.itemId}"
                            else -> operation.itemType
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        //modifier = Modifier.align(Alignment.Center)
                    )

                    //Spacer(modifier = Modifier.width(8.dp))




                Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$startTimeFormatted - $endTimeFormatted",
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formattedTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

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

            // Правый блок - кнопка завершения
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                // 3. Кнопка "Завершить" (справа)
                IconButton(
                    onClick = { onAction("COMPLETE") },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF388E3C))
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Завершить",
                        //modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }




        // Бейджи "Наличка" и "Скидка" (внизу карточки)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (operation.isCash) {
                Badge(
                    modifier = Modifier.padding(end = 2.dp),
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Text("Наличка", fontSize = 10.sp, color = Color.White)
                }
            }
            if (operation.hasDiscount) {
                Badge(
                    containerColor = Color(0xFF2196F3)
                ) {
                    Text("Скидка", fontSize = 10.sp, color = Color.White)
                }
            }

            // Бейдж с общим временем аренды
            if (operation.extensionsCount > 0) {
                Badge(
                    containerColor = Color(0xFFFFA000),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(formattedTotalDuration)
                }
            }
        }

            // Добавляем комментарий, если он есть
            if (operation.comment.isNotEmpty()) {
                Text(
                    text = "Комментарий: ${operation.comment}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

