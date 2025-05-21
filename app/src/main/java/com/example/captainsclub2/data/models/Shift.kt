package com.example.captainsclub2.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,  // Сохраняем дату в формате "dd.MM.yyyy"
    var status: Boolean,  // true = смена открыта, false = закрыта
    val matrosIds: String = ""  // Новое поле для хранения ID матросов через запятую (вместо List<Int>)
)