package com.example.captainsclub2.repository

import android.util.Log
import com.example.captainsclub2.data.database.AppDatabase
import com.example.captainsclub2.data.models.Matros
import com.example.captainsclub2.data.models.Operation
import com.example.captainsclub2.data.models.RentalExtension
import com.example.captainsclub2.data.models.Report
import com.example.captainsclub2.data.models.Shift
import com.example.captainsclub2.data.models.Souvenir
import com.example.captainsclub2.utils.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ShiftRepository(private val db: AppDatabase) {


    // Shift operations
    suspend fun getOpenShift() = db.shiftDao().getOpenShift()
    suspend fun saveShift(shift: Shift) = db.shiftDao().insert(shift)
    suspend fun closeShift(shift: Shift) {
        shift.status = false
        db.shiftDao().update(shift)
    }
    suspend fun updateShift(shift: Shift) {
        db.shiftDao().update(shift) // Предполагая, что update уже есть в ShiftDao
    }

    fun getOperationsForShift(shiftId: Int): Flow<List<Operation>> {
        return db.operationDao().getOperationsForShift(shiftId)
    }

    // Matros operations
    fun getAllMatrosFlow(): Flow<List<Matros>> = db.matrosDao().getAll()

    suspend fun getAllMatrosList(): List<Matros> = db.matrosDao().getAllList()

    suspend fun addMatros(matros: Matros) = db.matrosDao().insert(matros)
    suspend fun deleteMatros(matrosId: Int) {
        db.matrosDao().deleteById(matrosId)
    }
    suspend fun getMatrosName(id: Int): String {
        return db.matrosDao().getById(id)?.name ?: "Неизвестный"
    }

    fun getPermanentMatros(): Flow<List<Matros>> {
        return db.matrosDao().getPermanentMatros()
    }

    suspend fun updateMatros(matros: Matros) {
        db.matrosDao().updateMatros(matros)
    }

//    suspend fun getMatrosOnShift(shiftId: Int): List<Matros> {
//        return db.shiftMatrosDao().getMatrosForShift(shiftId)
//    }


//suspend fun debugPrintMatros() {
//    val matrosList = db.matrosDao().getAll()
//    println("DEBUG: Database matros content:")
//    matrosList.forEach {
//        println("ID: ${it.id}, Name: ${it.name}, Permanent: ${it.isPermanent}")
//    }
//}

    // Operation operations
    fun getActiveOperationsFlow(shiftId: Int): Flow<List<Operation>> {
        return db.operationDao().getActiveOperationsFlow(shiftId)
    }

    suspend fun getActiveOperations(shiftId: Int): List<Operation> {
        return db.operationDao().getActiveOperations(shiftId)
    }
    suspend fun saveOperation(operation: Operation) = db.operationDao().insert(operation)
    suspend fun updateOperation(operation: Operation) = db.operationDao().update(operation)
    suspend fun getOperationById(id: Int): Operation? {
        return db.operationDao().getById(id)
    }
    suspend fun getOperationsForGroup(groupId: String): List<Operation> {
        return db.operationDao().getOperationsForGroup(groupId)
    }



    //suspend fun getOperationsForShift(shiftId: Int): List<Operation>

    // Report operations
    suspend fun getRecentReports() = db.reportDao().getRecentReports()
    suspend fun saveReport(report: Report) = db.reportDao().insert(report)


    // Souvenir operations
    // 1. Метод для сохранения сувенира в БД
    suspend fun saveSouvenir(souvenir: Souvenir) {
        db.souvenirDao().insert(souvenir)
    }

    // 2. Метод для получения всех сувениров (Flow для реактивности)
    fun getAllSouvenirs(): Flow<List<Souvenir>> {
        return db.souvenirDao().getAllActive() // Добавим в SouvenirDao
    }

    // 3. Удаление (если нужно)
    suspend fun deleteSouvenir(name: String) {
        db.souvenirDao().deleteByName(name)
    }

    suspend fun souvenirExists(name: String): Boolean {
        return db.souvenirDao().exists(name)
    }

    suspend fun getSouvenirsCount(): Int {
        return db.souvenirDao().getCount()
    }

    suspend fun debugCheckSouvenirs() {
        try {
            val souvenirs = db.souvenirDao().getAllActive().first()
            Log.d("SOUVENIR_CHECK", "Souvenirs in DB: ${souvenirs.map { it.name }}")
        } catch (e: Exception) {
            Log.e("SOUVENIR_CHECK", "Error reading souvenirs", e)
        }
    }


    fun getReportDates(): Flow<List<String>> = db.reportDao().getReportDates()

    suspend fun getReportByDate(date: String): Report? = db.reportDao().getReportByDate(date)

    // 4. В ShiftRepository добавляем:
//    suspend fun saveReport(report: Report) {
//        db.reportDao().insert(report)
//    }

    fun getReportsByShiftId(shiftId: Int): Flow<List<Report>> {
        return db.reportDao().getByShiftId(shiftId)
    }


    // Добавляем методы для работы с продлениями
    suspend fun saveRentalExtension(extension: RentalExtension) {
        db.rentalExtensionDao().insert(extension)
    }

    suspend fun getOperationExtensions(opId: Int): List<RentalExtension> {
        return db.rentalExtensionDao().getExtensionsForOperation(opId)
    }

    suspend fun getGroupExtensions(groupId: String): List<RentalExtension> {
        return db.rentalExtensionDao().getExtensionsForGroup(groupId)
    }

    suspend fun getExtensionsCount(opId: Int): Int {
        return db.rentalExtensionDao().getExtensionsCountForOperation(opId)
    }

    suspend fun getExtensionsCountForOperation(opId: Int): Int {
        return db.rentalExtensionDao().getExtensionsCountForOperation(opId)
    }



    suspend fun getExtensionsForGroup(groupId: String): List<RentalExtension> {
        return db.rentalExtensionDao().getExtensionsForGroup(groupId)
    }

    suspend fun getAllExtensionsForShift(shiftId: Int): List<RentalExtension> {
        return db.rentalExtensionDao().getExtensionsForShift(shiftId)
    }

    fun getExtensionsForShiftFlow(shiftId: Int): Flow<List<RentalExtension>> {
        return db.rentalExtensionDao().getExtensionsForShiftFlow(shiftId)
    }

    suspend fun getExtensionsForOperation(opId: Int): List<RentalExtension> {
        return db.rentalExtensionDao().getExtensionsForOperation(opId)
    }

    suspend fun updateRentalExtension(extension: RentalExtension) {
        db.rentalExtensionDao().updateRentalExtension(extension)
    }

    suspend fun getRentalExtensionById(id: Int): RentalExtension? {
        return db.rentalExtensionDao().getById(id)
    }

    // Обновленный метод для создания операции с учетом продлений
//    suspend fun createRentalExtension(
//        originalOp: Operation,
//        isGroup: Boolean = false,
//        groupId: String? = null
//    ): Operation {
//        val extension = RentalExtension(
//            type = if (isGroup) "GROUP" else "SINGLE",
//            parentOperationId = if (!isGroup) originalOp.id else null,
//            parentGroupId = groupId,
//            targetOperationId = if (isGroup) originalOp.id else null,
//            price = originalOp.price,
//            isCash = originalOp.isCash,
//            hasDiscount = originalOp.hasDiscount,
//            shiftId = originalOp.shiftId
//        ).also { db.rentalExtensionDao().insert(it) }
//
//        // Создаем операцию для отчета
//        return Operation(
//            type = "RENTAL_EXTENSION",
//            itemType = originalOp.itemType,
//            itemId = originalOp.itemId,
//            shiftId = originalOp.shiftId,
//            startTime = TimeUtils.getCurrentTime(),
//            startTimestamp = System.currentTimeMillis(),
//            status = "ACTIVE",
//            isCash = originalOp.isCash,
//            hasDiscount = originalOp.hasDiscount,
//            price = originalOp.price,
//            parentExtensionId = extension.id // Связь с продлением
//        ).also { db.operationDao().insert(it) }
//    }
}




