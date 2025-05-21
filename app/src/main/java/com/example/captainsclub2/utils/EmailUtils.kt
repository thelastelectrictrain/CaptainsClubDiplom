package com.example.captainsclub2.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.captainsclub2.utils.TimeUtils.getCurrentDate

object EmailUtils {
    private const val DEFAULT_EMAIL = "ooobestpark@gmail.com" // Основной email получателя
    private const val DEFAULT_SUBJECT = "Отчет о смене"

    /**
     * Для отправки отчета при закрытии смены (использует настройки по умолчанию)
     */
    fun sendReportEmail(context: Context, reportText: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("ooobestpark@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Отчёт о смене ${getCurrentDate()}")
                putExtra(Intent.EXTRA_TEXT, reportText)
            }

            context.startActivity(
                Intent.createChooser(intent, "Отправить отчёт")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка отправки: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // Для архивных отчетов
    fun sendArchiveReport(context: Context, report: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(DEFAULT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "Архивный отчет о смене")
                putExtra(Intent.EXTRA_TEXT, report)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(
                Intent.createChooser(intent, "Отправить архивный отчет")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка отправки: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // Для текущего отчета
    fun sendCurrentReport(context: Context, report: String) {
        sendEmail(context, DEFAULT_EMAIL, DEFAULT_SUBJECT, report)
    }

    /**
     * Универсальный метод для отправки email
     */
    fun sendEmail(
        context: Context,
        email: String,
        subject: String,
        body: String
    ) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            context.startActivity(Intent.createChooser(intent, "Отправить отчет"))
        } catch (e: Exception) {
            Toast.makeText(context, "Не найдено приложение для отправки email", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Удаляем ненужные методы (isNetworkAvailable и дублирующие sendEmail)
}
