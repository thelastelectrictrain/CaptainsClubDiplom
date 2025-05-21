package com.example.captainsclub2.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatTimeLong(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    fun formatTimeForDisplay(time: String): String {
        return try {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(time) ?: return time
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            time.take(5) // Fallback - берем первые 5 символов
        }
    }

    fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun parseTimeToMillis(time: String): Long {
        return try {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.parse(time)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    fun calculateEndTime(startTime: String, minutesToAdd: Int): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(startTime) ?: return ""

        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MINUTE, minutesToAdd)

        return sdf.format(calendar.time)
    }

    fun getCurrentTimeFlow(): Flow<String> = flow {
        while (true) {
            emit(getCurrentTime())
            delay(1000) // Обновляем каждую секунду
        }
    }
}