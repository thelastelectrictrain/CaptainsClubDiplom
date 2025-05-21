package com.example.captainsclub2.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.captainsclub2.data.database.AppDatabase
import com.example.captainsclub2.repository.ShiftRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "captains-db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // Добавляем обе миграции
            .fallbackToDestructiveMigration() // Временная мера для разработки
            .build()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                Log.d("DB_MIGRATION", "Executing migration 1_2")

                // 1. Создаем таблицу
                database.execSQL("""
                CREATE TABLE IF NOT EXISTS souvenirs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL UNIQUE,
                    price REAL NOT NULL DEFAULT 0,
                    isActive INTEGER NOT NULL DEFAULT 1
                )
            """)

                // 2. Добавляем дефолтные сувениры
                val defaultSouvenirs = listOf(
                    "Шапка" to 500.0,
                    "Брелок" to 350.0,
                    "Верёвка" to 100.0,
                    "Утка (продажа)" to 2500.0,
                    "Статуэтка" to 500.0
                )

                defaultSouvenirs.forEach { (name, price) ->
                    database.execSQL("""
                    INSERT OR IGNORE INTO souvenirs (name, price, isActive)
                    VALUES ('$name', $price, 1)
                """)
                    Log.d("DB_MIGRATION", "Added souvenir: $name")
                }
            } catch (e: Exception) {
                Log.e("DB_MIGRATION", "Error in migration", e)
            }
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE Operation ADD COLUMN startTimestamp INTEGER DEFAULT 0")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                Log.d("DB_MIGRATION", "Executing migration 3_4")

                // 1. Создаем таблицу для продлений
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS rental_extensions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        durationMinutes INTEGER NOT NULL DEFAULT 20,
                        price REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        isCash INTEGER NOT NULL DEFAULT 0,
                        hasDiscount INTEGER NOT NULL DEFAULT 0,
                        parentOperationId INTEGER,
                        parentGroupId TEXT,
                        targetOperationId INTEGER,
                        shiftId INTEGER NOT NULL
                    )
                """)

                // 2. Добавляем колонку parentExtensionId в Operation
                database.execSQL("""
                    ALTER TABLE Operation 
                    ADD COLUMN parentExtensionId INTEGER DEFAULT NULL
                """)

                // 3. Добавляем колонку extensionsCount в Operation
                database.execSQL("""
                    ALTER TABLE Operation 
                    ADD COLUMN extensionsCount INTEGER NOT NULL DEFAULT 0
                """)

                Log.d("DB_MIGRATION", "Migration 3_4 completed successfully")
            } catch (e: Exception) {
                Log.e("DB_MIGRATION", "Error in migration 3_4", e)
                throw e // Важно прокинуть исключение дальше
            }
        }
    }

    @Provides
    fun provideShiftRepository(db: AppDatabase): ShiftRepository {
        return ShiftRepository(db)
    }
}




