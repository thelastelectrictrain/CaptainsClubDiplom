package com.example.captainsclub2.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.captainsclub2.data.models.Matros
import com.example.captainsclub2.data.models.Operation
import com.example.captainsclub2.data.models.RentalExtension
import com.example.captainsclub2.data.models.Report
import com.example.captainsclub2.data.models.Shift
import com.example.captainsclub2.data.models.Souvenir
import kotlinx.coroutines.flow.Flow

@Database(entities = [Shift::class, Matros::class, Operation::class, RentalExtension::class, Report::class, Souvenir::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun matrosDao(): MatrosDao
    abstract fun operationDao(): OperationDao
    abstract fun rentalExtensionDao(): RentalExtensionDao // Новый DAO
    abstract fun reportDao(): ReportDao
    abstract fun souvenirDao(): SouvenirDao
}

// DAO for Shift
@Dao
interface ShiftDao {
    @Query("SELECT * FROM Shift WHERE status = 1 LIMIT 1")
    suspend fun getOpenShift(): Shift?



    @Insert
    suspend fun insert(shift: Shift)

    @Update
    suspend fun update(shift: Shift)
}

// DAO for Matros
@Dao
interface MatrosDao {
    @Query("SELECT * FROM Matros")
    fun getAll(): Flow<List<Matros>>

    @Query("SELECT * FROM Matros")
    suspend fun getAllList(): List<Matros>

    @Query("SELECT * FROM Matros WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Matros?

    @Insert
    suspend fun insert(matros: Matros)

    @Query("DELETE FROM Matros WHERE id = :matrosId")
    suspend fun deleteById(matrosId: Int)

    @Delete
    suspend fun delete(matros: Matros)

    @Query("SELECT * FROM Matros WHERE isPermanent = 1")
    fun getPermanentMatros(): Flow<List<Matros>>

    @Update
    suspend fun updateMatros(matros: Matros)
}

// DAO for Operation
@Dao
interface OperationDao {

    @Query("SELECT * FROM Operation WHERE status = 'ACTIVE' AND shiftId = :shiftId")
    fun getActiveOperationsFlow(shiftId: Int): Flow<List<Operation>>

    @Query("SELECT * FROM Operation WHERE status = 'ACTIVE' AND shiftId = :shiftId")
    suspend fun getActiveOperations(shiftId: Int): List<Operation>

    @Query("SELECT * FROM Operation WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Operation?

    @Query("SELECT * FROM Operation WHERE shiftId = :shiftId ORDER BY startTime DESC")
    fun getOperationsForShift(shiftId: Int): Flow<List<Operation>>

    @Insert
    suspend fun insert(operation: Operation)

    @Update
    suspend fun update(operation: Operation)

    @Query("SELECT * FROM Operation WHERE isExtension = 0") // Для фильтрации
    fun getNonExtensionOperations(): Flow<List<Operation>>

    @Query("SELECT * FROM Operation WHERE groupId = :groupId")
    suspend fun getOperationsForGroup(groupId: String): List<Operation>
}

// Новый DAO для продлений
@Dao
interface RentalExtensionDao {
    @Insert
    suspend fun insert(extension: RentalExtension)

    @Update
    suspend fun update(extension: RentalExtension)

    @Update
    suspend fun updateRentalExtension(extension: RentalExtension)

    @Query("SELECT * FROM rental_extensions WHERE parentOperationId = :opId")
    suspend fun getExtensionsForOperation(opId: Int): List<RentalExtension>

    @Query("SELECT * FROM rental_extensions WHERE parentGroupId = :groupId")
    suspend fun getExtensionsForGroup(groupId: String): List<RentalExtension>

    @Query("SELECT COUNT(*) FROM rental_extensions WHERE parentOperationId = :opId")
    suspend fun getExtensionsCountForOperation(opId: Int): Int

    @Query("SELECT * FROM rental_extensions WHERE shiftId = :shiftId")
    suspend fun getExtensionsForShift(shiftId: Int): List<RentalExtension>

//    @Query("SELECT * FROM Operation WHERE groupId = :groupId")
//    suspend fun getOperationsForGroup(groupId: String): List<Operation>

    @Query("SELECT * FROM rental_extensions WHERE shiftId = :shiftId")
    fun getExtensionsForShiftFlow(shiftId: Int): Flow<List<RentalExtension>>

    @Query("SELECT * FROM rental_extensions WHERE id = :id")
    suspend fun getById(id: Int): RentalExtension?
}

// DAO for Report
@Dao
interface ReportDao {
    @Query("SELECT * FROM Report ORDER BY id DESC LIMIT 220")
    suspend fun getRecentReports(): List<Report>

    @Insert
    suspend fun insert(report: Report)

    @Query("SELECT * FROM Report WHERE shiftId = :shiftId ORDER BY id DESC")
    fun getByShiftId(shiftId: Int): Flow<List<Report>>

    @Query("SELECT DISTINCT s.date FROM Report r JOIN Shift s ON r.shiftId = s.id ORDER BY s.date DESC")
    fun getReportDates(): Flow<List<String>>

    @Query("SELECT r.* FROM Report r JOIN Shift s ON r.shiftId = s.id WHERE s.date = :date LIMIT 1")
    suspend fun getReportByDate(date: String): Report?
}

@Dao
interface SouvenirDao {
    @Query("SELECT * FROM souvenirs WHERE isActive = 1")
    fun getAllActive(): Flow<List<Souvenir>>

    @Insert
    suspend fun insert(souvenir: Souvenir)

    @Query("UPDATE souvenirs SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Int)


    @Query("SELECT * FROM souvenirs ORDER BY name ASC")
    fun getAllFlow(): Flow<List<Souvenir>>


    @Query("SELECT name FROM souvenirs")
    fun getAllNamesFlow(): Flow<List<String>> // Реактивный запрос

    @Query("SELECT EXISTS(SELECT * FROM souvenirs WHERE name = :name)")
    suspend fun exists(name: String): Boolean

    @Query("SELECT name FROM souvenirs ORDER BY name ASC")
    suspend fun getAllNames(): List<String>

    @Query("DELETE FROM souvenirs WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT * FROM souvenirs ORDER BY name")
    fun getAll(): Flow<List<Souvenir>>

    @Query("SELECT COUNT(*) FROM souvenirs")
    suspend fun getCount(): Int

}