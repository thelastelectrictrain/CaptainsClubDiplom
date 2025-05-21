package com.example.captainsclub2.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Operation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: String = "",
    val type: String, // "RENTAL", "SALE", "FINE"
    val itemType: String, // "DUCK", "YACHT", "SOUVENIR", etc.
    val itemId: Int, // Item ID (e.g., duck number)
    val shiftId: Int, // Привязка к текущей смене
    val startTime: String, // Format: HH:mm:ss
    val startTimestamp: Long = System.currentTimeMillis(), // NEW! Метка времени создания
    val endTime: String = "", // Format: HH:mm:ss
    val status: String, // "COMPLETED", "CANCELED"
    val isCash: Boolean = false,
    val hasDiscount: Boolean = false,
    val price: Double = 0.0,
    val comment: String = "",
    val extensionsCount: Int = 0, // Новое поле для хранения количества продлений
    val isExtension: Boolean = false, // Новое поле для пометки операций продления
    val parentExtensionId: Int? = null, // Ссылка на RentalExtension
) {
    //fun isExtension() = parentExtensionId != null
}