package com.example.captainsclub2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarDialog(
    availableDates: List<String>,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedDate by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    fun getDaysInMonth(): Int {
        val tempCalendar = Calendar.getInstance()
        tempCalendar.set(Calendar.YEAR, currentYear)
        tempCalendar.set(Calendar.MONTH, currentMonth)
        return tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getFirstDayOfWeek(): Int {
        val tempCalendar = Calendar.getInstance()
        tempCalendar.set(Calendar.YEAR, currentYear)
        tempCalendar.set(Calendar.MONTH, currentMonth)
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)
        // Calendar.DAY_OF_WEEK: 1-7 (воскресенье-суббота)
        // Конвертируем к 0-6 (понедельник-воскресенье)
        return (tempCalendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    fun getCurrentMonthName(): String {
        val tempCalendar = Calendar.getInstance()
        tempCalendar.set(Calendar.MONTH, currentMonth)
        tempCalendar.set(Calendar.YEAR, currentYear)
        return monthFormat.format(tempCalendar.time)
    }

    fun isDateAvailable(day: Int): Boolean {
        val tempCalendar = Calendar.getInstance()
        tempCalendar.set(Calendar.YEAR, currentYear)
        tempCalendar.set(Calendar.MONTH, currentMonth)
        tempCalendar.set(Calendar.DAY_OF_MONTH, day)
        val dateStr = dateFormat.format(tempCalendar.time)
        return availableDates.contains(dateStr)
    }

    fun getDateString(day: Int): String {
        val tempCalendar = Calendar.getInstance()
        tempCalendar.set(Calendar.YEAR, currentYear)
        tempCalendar.set(Calendar.MONTH, currentMonth)
        tempCalendar.set(Calendar.DAY_OF_MONTH, day)
        return dateFormat.format(tempCalendar.time)
    }

    fun isToday(day: Int): Boolean {
        val today = Calendar.getInstance()
        return currentYear == today.get(Calendar.YEAR) &&
                currentMonth == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Заголовок с навигацией по месяцам
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (currentMonth == Calendar.JANUARY) {
                                currentMonth = Calendar.DECEMBER
                                currentYear--
                            } else {
                                currentMonth--
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущий месяц")
                    }

                    Text(
                        text = getCurrentMonthName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = {
                            if (currentMonth == Calendar.DECEMBER) {
                                currentMonth = Calendar.JANUARY
                                currentYear++
                            } else {
                                currentMonth++
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Следующий месяц")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Дни недели
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Дни месяца
                val daysInMonth = getDaysInMonth()
                val firstDayOfWeek = getFirstDayOfWeek()
                val days = List(42) { index ->
                    if (index >= firstDayOfWeek && index < firstDayOfWeek + daysInMonth) {
                        index - firstDayOfWeek + 1
                    } else {
                        null
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(240.dp)
                ) {
                    items(days.size) { index ->
                        val day = days[index]
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .then(
                                    if (day != null && isDateAvailable(day)) {
                                        Modifier.clickable {
                                            selectedDate = getDateString(day)
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != null) {
                                val dateAvailable = isDateAvailable(day)
                                val today = isToday(day)
                                Text(
                                    text = day.toString(),
                                    color = when {
                                        !dateAvailable -> Color.Gray.copy(alpha = 0.5f)
                                        selectedDate == getDateString(day) -> MaterialTheme.colorScheme.primary
                                        today -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (selectedDate == getDateString(day) || today) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            selectedDate?.let { onDateSelected(it) }
                        },
                        enabled = selectedDate != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D47A1)
                        )
                    ) {
                        Text("Выбрать")
                    }
                }
            }
        }
    }
}