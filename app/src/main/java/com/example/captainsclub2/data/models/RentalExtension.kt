package com.example.captainsclub2.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.captainsclub2.data.models.Operation
import com.example.captainsclub2.utils.TimeUtils

@Entity(tableName = "rental_extensions")
data class RentalExtension(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "SINGLE" или "GROUP"
    val durationMinutes: Int = 20,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE",
    val isCash: Boolean = false,
    val hasDiscount: Boolean = false,
    val parentOperationId: Int? = null,
    val parentGroupId: String? = null,
    val targetOperationId: Int? = null,
    val originalOperationId: Int, // ID основной аренды
    val sequenceNumber: Int = 1, // Порядковый номер продления
    val shiftId: Int // Привязка к смене
)
{
    fun toReportOperation(): Operation {
    return Operation(
        type = "RENTAL_EXTENSION",
        itemType = when {
            parentGroupId != null -> "GROUP_EXTENSION"
            else -> "SOLO_EXTENSION"
        },
        itemId = targetOperationId ?: parentOperationId ?: 0,
        shiftId = shiftId,
        startTime = TimeUtils.formatTimeLong(timestamp),
        startTimestamp = timestamp,
        status = status,
        isCash = isCash,
        hasDiscount = hasDiscount,
        price = price,
        parentExtensionId = id,
        extensionsCount = 0 // Продления сами не могут иметь продлений
    )
}
    fun toDisplayOperation(original: Operation): Operation {
        return original.copy(
            id = this.id, // или другой уникальный ID
            type = "RENTAL_EXTENSION",
            startTimestamp = this.timestamp,
            startTime = TimeUtils.formatTimeLong(this.timestamp),
            isExtension = true,
            parentExtensionId = this.id
        )
    }
}