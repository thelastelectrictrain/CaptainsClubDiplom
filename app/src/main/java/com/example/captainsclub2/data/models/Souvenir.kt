package com.example.captainsclub2.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "souvenirs")
data class Souvenir(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val price: Double = 0.0, // Можно добавить если нужно
    val isActive: Boolean = true
)