package com.example.captainsclub2.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.captainsclub2.data.models.Matros
import com.example.captainsclub2.data.models.Shift
import com.example.captainsclub2.data.models.Report
import com.example.captainsclub2.repository.ShiftRepository
import com.example.captainsclub2.utils.EmailUtils
import com.example.captainsclub2.utils.TimeUtils.getCurrentDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.captainsclub2.data.models.Operation
import com.example.captainsclub2.data.models.RentalExtension
import com.example.captainsclub2.data.models.Souvenir
import com.example.captainsclub2.utils.TimeUtils
import com.example.captainsclub2.utils.TimeUtils.getCurrentTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.example.captainsclub2.utils.LogTags
import com.example.captainsclub2.utils.LogTags.TAG_EXTENSION
import com.example.captainsclub2.utils.LogTags.TAG_OPERATION
import com.example.captainsclub2.utils.LogTags.TAG_REPORT
import kotlinx.coroutines.runBlocking

//private const val TAG_OPERATION = "OperationFlow"
//private const val TAG_EXTENSION = "ExtensionFlow"
//private const val TAG_REPORT = "ReportCalculation"

@HiltViewModel
class MainViewModel @Inject constructor
    (
    private val repository: ShiftRepository
            ) : ViewModel() {



    //private val _currentShift = MutableStateFlow<Shift?>(null)
    //val currentShift: StateFlow<Shift?> = _currentShift

    private val _matrosState = MutableStateFlow<MatrosState>(MatrosState.Loading)
    val matrosState: StateFlow<MatrosState> = _matrosState

    private val _currentShift = MutableStateFlow<Shift?>(null)
    private val _operations = MutableStateFlow<List<Operation>>(emptyList())
    private val _matrosList = MutableStateFlow<List<Matros>>(emptyList())
    private val _allMatros = MutableStateFlow<List<Matros>>(emptyList())

    val currentShift: StateFlow<Shift?> = _currentShift.asStateFlow()

    val operations: StateFlow<List<Operation>> = _currentShift
        .flatMapLatest { shift ->
            shift?.let {
                repository.getOperationsForShift(it.id)
            } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Добавляем StateFlow для продлений

    val rentalExtensions: StateFlow<List<RentalExtension>> = _currentShift
        .flatMapLatest { shift ->
            shift?.let { repository.getExtensionsForShiftFlow(it.id) }
                ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Добавьте эти методы в MainViewModel
    private val _operationNumbers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val operationNumbers: StateFlow<Map<Int, Int>> = _operationNumbers

    fun getOriginalOperation(extension: Operation): Operation? {
        return runBlocking {
            extension.parentExtensionId?.let { extensionId ->
                repository.getRentalExtensionById(extensionId)?.let { ext ->
                    repository.getOperationById(ext.originalOperationId)
                }
            }
        }
    }

    // В init или отдельном методе:
    private fun setupOperationNumbers() {
        viewModelScope.launch {
            combine(
                operations,
                rentalExtensions
            ) { ops, exts ->
                val allOps = ops + exts.map { it.toReportOperation() }
                allOps
                    .filter { it.status == "COMPLETED" }
                    .sortedBy { it.startTimestamp }
                    .mapIndexed { index, op -> op.id to (index + 1) }
                    .toMap()
            }.collect { numbers ->
                _operationNumbers.value = numbers
            }
        }
    }

    // val operations: StateFlow<List<Operation>> = _operations.asStateFlow()
    val matrosList: StateFlow<List<Matros>> = _matrosList.asStateFlow()

    private val _permanentMatros = MutableStateFlow<List<Matros>>(emptyList())
    //val permanentMatros: StateFlow<List<Matros>> = _permanentMatros.asStateFlow()

    private val _matrosOnShift = MutableStateFlow<List<Matros>>(emptyList())
    //val matrosOnShift: StateFlow<List<Matros>> = _matrosOnShift.asStateFlow()

    private val defaultMatrosNames = listOf(
        "Дима У.", "Малик", "Камилла 23",
        "Камила 22", "Саша лвл", "Диля", "Ростислав"
    )


    val permanentMatros: StateFlow<List<Matros>> =
        combine(_allMatros, _currentShift) { allMatros, shift ->
            val onShiftIds = shift?.matrosIds?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            allMatros.filter { matros ->
                matros.isPermanent && !onShiftIds.contains(matros.id.toString())
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val matrosOnShift: StateFlow<List<Matros>> =
        combine(_allMatros, _currentShift) { allMatros, shift ->
            val onShiftIds = shift?.matrosIds?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            allMatros.filter { matros ->
                onShiftIds.contains(matros.id.toString())
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun loadMatrosData() {
        viewModelScope.launch {
            repository.getAllMatrosFlow().collect { allMatros ->
                _permanentMatros.value = allMatros.filter { it.isPermanent }

                _currentShift.value?.let { shift ->
                    val onShiftIds = shift.matrosIds.split(",").filter { it.isNotEmpty() }
                    _matrosOnShift.value = allMatros.filter { matros ->
                        onShiftIds.contains(matros.id.toString())
                    }
                }
            }
        }
    }

    private fun seedDefaultMatros() {
        viewModelScope.launch {
            val existingMatros = repository.getAllMatrosList() // Используем List-версию
            println("DEBUG: Existing matros count = ${existingMatros.size}")

            if (existingMatros.isEmpty()) {
                defaultMatrosNames.forEach { name ->
                    repository.addMatros(Matros(name = name, isPermanent = true))
                }
            }
        }
    }


    // работа проверки матросов не смене
    private val _forceMatrosTab = MutableStateFlow(false)
    val forceMatrosTab: StateFlow<Boolean> = _forceMatrosTab

//    private val _showNoMatrosWarning = MutableSharedFlow<Unit>()
//    val showNoMatrosWarning: SharedFlow<Unit> = _showNoMatrosWarning

    init {
        // 1. Загрузка активной смены
        viewModelScope.launch {
            _currentShift.value = repository.getOpenShift()
            if (_currentShift.value?.matrosIds.isNullOrEmpty()) {
                _forceMatrosTab.value = true
            }
        }

        // 2. Загрузка всех матросов (штат команды)
//        viewModelScope.launch {
//            _matrosList.value = repository.getAllMatros()
//        }
        //loadMatrosData()
        loadInitialData()
        seedDefaultMatros()

        viewModelScope.launch {
            repository.getAllMatrosFlow().collect { allMatros ->
                println("DEBUG: Loaded matros: ${allMatros.map { it.name }}") // Лог
                _permanentMatros.value = allMatros.filter { it.isPermanent }

                _currentShift.value?.let { shift ->
                    val onShiftIds = shift.matrosIds.split(",").filter { it.isNotEmpty() }
                    _matrosOnShift.value = allMatros.filter { matros ->
                        onShiftIds.contains(matros.id.toString())
                    }
                }
            }
        }

        // 3. Реактивная подписка на операции активной смены
        viewModelScope.launch {
            _currentShift.collect { shift ->
                shift?.let {
                    repository.getActiveOperationsFlow(it.id).collect { ops ->
                        _operations.value = ops
                    }
                } ?: run { _operations.value = emptyList() }
            }
        }

        viewModelScope.launch {
            // 1. Проверяем базу
            val count = repository.getSouvenirsCount()
            Log.d("DB_CHECK", "Initial souvenirs count: $count")

            // 2. Если сувениров нет - добавляем дефолтные
            if (count == 0) {
                Log.d("DB_INIT", "Adding default souvenirs")
                listOf(
                    Souvenir(name = "Шапка", price = 500.0),
                    Souvenir(name = "Брелок", price = 350.0),
                    Souvenir(name = "Верёвка", price = 100.0),
                    Souvenir(name = "Утка (продажа)", price = 2500.0),
                    Souvenir(name = "Статуэтка", price = 500.0)
                ).forEach { repository.saveSouvenir(it) }
            }
        }
    }

//    fun showNoMatrosWarning() {
//        viewModelScope.launch {
//            _showNoMatrosWarning.emit(Unit)
//        }
//    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Загружаем смену
            _currentShift.value = repository.getOpenShift()

            // Подписываемся на изменения матросов
            repository.getAllMatrosFlow().collect { matrosList ->
                _allMatros.value = matrosList
            }
        }
    }

    private val _lastSeenRentalIndex = MutableStateFlow(-1)
    val lastSeenRentalIndex: StateFlow<Int> = _lastSeenRentalIndex

    fun updateLastSeenIndex(index: Int) {
        _lastSeenRentalIndex.value = index
    }

    // Изменяем метод startRental
    suspend fun startRental(type: String, itemType: String, itemId: Int): Operation {
        val currentShift = _currentShift.value ?: throw IllegalStateException("No active shift")
        val operation = Operation(
            type = type,
            itemType = itemType,
            itemId = itemId,
            shiftId = currentShift.id,
            startTime = TimeUtils.getCurrentTime(),
            startTimestamp = System.currentTimeMillis(),
            status = "ACTIVE",
            isCash = false,
            hasDiscount = false,
            price = 350.0
        )
        repository.saveOperation(operation)
        return operation
    }

    // Обновленный метод для получения операций
    val groupedRentals: StateFlow<Map<String, List<Operation>>> = operations
        .map { ops ->
            ops.filter { it.type == "RENTAL" && it.status == "ACTIVE" }
                .groupBy { it.groupId }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // В разделе с другими методами аренды
    suspend fun createGroupRental(type: String, itemType: String, numbers: List<Int>): String {
        val groupId = UUID.randomUUID().toString()
        numbers.forEach { number ->
            val operation = Operation(
                type = type,
                itemType = itemType,
                itemId = number,
                shiftId = currentShift.value?.id ?: 0,
                startTime = TimeUtils.getCurrentTime(),
                startTimestamp = System.currentTimeMillis(),
                status = "ACTIVE",
                isCash = false,
                hasDiscount = false,
                price = 350.0,
                groupId = groupId
            )
            repository.saveOperation(operation)
        }
        return groupId
    }


    suspend fun updateRentalExtensions(opId: Int, isCash: Boolean, hasDiscount: Boolean) {
        Log.d(LogTags.TAG_EXTENSION, """
        Updating extensions for op $opId:
        New cash state: $isCash, New discount: $hasDiscount
    """.trimIndent())

        val extensions = repository.getExtensionsForOperation(opId)
        val newPrice = if (hasDiscount) 175.0 else 350.0 // Пересчитываем цену

        extensions.forEach { ext ->

            Log.d(TAG_EXTENSION, """
            Updating extension ${ext.id}:
            Old cash: ${ext.isCash}, Old discount: ${ext.hasDiscount}
            New price: $newPrice
        """.trimIndent())

            repository.updateRentalExtension(
                ext.copy(
                    isCash = isCash,
                    hasDiscount = hasDiscount,
                    price = newPrice // Обновляем цену во всех продлениях
                )
            )
            Log.d("EXTENSION_UPDATE", "Updated extension ${ext.id}: cash=$isCash")
        }
    }

    suspend fun updateGroupExtensions(groupId: String, isCash: Boolean, hasDiscount: Boolean) {
        val groupExtensions = repository.getExtensionsForGroup(groupId)
        groupExtensions.forEach { ext ->
            repository.updateRentalExtension(
                ext.copy(
                    isCash = isCash,
                    hasDiscount = hasDiscount
                )
            )
        }
    }

    suspend fun updateRentalDiscount(operationId: Int, hasDiscount: Boolean) {
        repository.getOperationById(operationId)?.let { operation ->
            val newPrice = if (hasDiscount) 175.0 else 350.0 // Явно пересчитываем цену
            repository.updateOperation(
                operation.copy(
                    hasDiscount = hasDiscount,
                    price = newPrice // Всегда обновляем цену
                )
            )
        }
    }

    // Добавляем метод для расчета оставшегося времени
    fun calculateRemainingTime(operation: Operation): Int {
        return when {
            operation.status != "ACTIVE" -> 0
            operation.startTimestamp <= 0 -> 1380 // Для старых записей (23 минуты)
            else -> {
                val elapsed =
                    ((System.currentTimeMillis() - operation.startTimestamp) / 1000).toInt()
                (1380 - elapsed).coerceAtLeast(0)
            }
        }
    }


    fun saveOperation(operation: Operation) {
        viewModelScope.launch {
            repository.saveOperation(operation)
        }
    }


    // Метод для продления одиночной аренды
    suspend fun extendSingleRental(operation: Operation) {
        val extensionsCount = repository.getExtensionsCountForOperation(operation.id)

        // Важно: берем текущую цену с учетом скидки
        val extensionPrice = if (operation.hasDiscount) 175.0 else 350.0

        Log.d(TAG_EXTENSION, """
        Creating extension for op ${operation.id}:
        Base price: ${operation.price}, Discount: ${operation.hasDiscount}
        New price: $extensionPrice, Sequence: ${extensionsCount + 1}
    """.trimIndent())

        val extension = RentalExtension(
            originalOperationId = operation.id,
            sequenceNumber = extensionsCount + 1,
            type = "SINGLE",
            parentOperationId = operation.id,
            price = extensionPrice, // Используем цену с учетом текущей скидки
            shiftId = operation.shiftId,
            isCash = operation.isCash, // Сохраняем текущее состояние налички
            hasDiscount = operation.hasDiscount, // Сохраняем текущее состояние скидки
            timestamp = System.currentTimeMillis()
        ).also {
            repository.saveRentalExtension(it)
            Log.d("EXTENSION_DEBUG", "Saved extension: ${it.id}, cash: ${it.isCash}, price: ${it.price}")
        }

        // Обновляем счетчик продлений у исходной операции
        repository.updateOperation(
            operation.copy(extensionsCount = operation.extensionsCount + 1)
        )
        Log.d(TAG_OPERATION, "Updated extensions count for op ${operation.id}: ${extensionsCount + 1}")
    }

    // Метод для продления групповой аренды
    suspend fun extendGroupRental(groupId: String) {
        // 1. Получаем все операции группы
        val groupOps = repository.getOperationsForGroup(groupId)
        if (groupOps.isEmpty()) return

        // 2. Создаем продления для каждой операции в группе
        groupOps.forEach { op ->
            val extensionsCount = repository.getExtensionsCountForOperation(op.id)

            val extension = RentalExtension(
                originalOperationId = op.id,
                sequenceNumber = extensionsCount + 1,
                type = "GROUP",
                parentGroupId = groupId,
                price = op.price,
                shiftId = op.shiftId,
                isCash = op.isCash,
                hasDiscount = op.hasDiscount,
                timestamp = System.currentTimeMillis()
            ).also {
                repository.saveRentalExtension(it)
            }

            // 3. Обновляем счетчик у исходной операции
            repository.updateOperation(
                op.copy(extensionsCount = op.extensionsCount + 1)
            )
        }

        // 4. Обновляем UI
        _operations.value = repository.getActiveOperations(groupOps.first().shiftId)
    }


    suspend fun updateOperation(operation: Operation) {

        val currentOp = repository.getOperationById(operation.id) ?: return

        val updatedOp = currentOp.copy(
            isCash = operation.isCash,
            hasDiscount = operation.hasDiscount,
            price = if (operation.hasDiscount) 175.0 else 350.0,
            comment = operation.comment
        )

        repository.updateOperation(updatedOp)

        Log.d(TAG_OPERATION, """
        Updating operation ${operation.id}:
        Type: ${operation.itemType} ${operation.itemId}
        Cash: ${operation.isCash}, Discount: ${operation.hasDiscount}
        Price: ${operation.price}, Extensions: ${operation.extensionsCount}
    """.trimIndent())

        // Обновляем все связанные продления
        if (operation.isCash != currentOp.isCash || operation.hasDiscount != currentOp.hasDiscount) {
            Log.d("UPDATE_DEBUG", "Cash changed from ${currentOp.isCash} to ${operation.isCash}")
            updateRentalExtensions(operation.id, operation.isCash, operation.hasDiscount)
        }

        //repository.updateOperation(operation)
    }

    suspend fun replaceRentalItem(operationId: Int, newItemId: Int) {
        val currentOperations = operations.value
        val operation = repository.getOperationById(operationId) ?: return

        // Проверяем, что новый номер того же типа
        val newItemType = getItemType(newItemId, currentOperations)
        if (newItemType != null && newItemType != operation.itemType) {
            Log.d("REPLACEMENT", "Cannot replace ${operation.itemType} with $newItemType")
            return
        }

        // Проверка что это замена на другой номер
        if (operation.itemId == newItemId) {
            Log.d("REPLACEMENT", "Attempt to replace with same ID")
            return
        }

        // Проверка что номер свободен (кроме текущего)
        val isOccupied = currentOperations
            .filter { it.id != operationId }
            .any { it.itemId == newItemId && it.status == "ACTIVE" }

        if (isOccupied) {
            Log.d("REPLACEMENT", "Item $newItemId is already occupied")
            return
        }

        Log.d("REPLACEMENT", "Replacing item ${operation.itemId} with $newItemId in operation $operationId")
        repository.updateOperation(operation.copy(itemId = newItemId))

        // Обновляем продления
        repository.getExtensionsForOperation(operationId).forEach { ext ->
            repository.updateRentalExtension(ext.copy())
        }
    }

    suspend fun replaceGroupItem(groupId: String, oldItemId: Int, newItemId: Int) {
        Log.d("REPLACEMENT", "Replacing item $oldItemId with $newItemId in group $groupId")
        val groupOps = repository.getOperationsForGroup(groupId)

        groupOps.firstOrNull { it.itemId == oldItemId }?.let { op ->
            replaceRentalItem(op.id, newItemId)
        }
    }

    private fun getItemType(itemId: Int, operations: List<Operation>): String? {
        // Находим операцию с этим itemId (если есть)
        val operation = operations.firstOrNull { it.itemId == itemId }
        return operation?.itemType
    }

    private fun loadMatros() {
        viewModelScope.launch {
            try {
                val matrosList = repository.getAllMatrosList() // Используем List-версию
                _matrosState.value = MatrosState.Success(matrosList)
            } catch (e: Exception) {
                _matrosState.value = MatrosState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createMatros(name: String) {
        viewModelScope.launch {
            repository.addMatros(Matros(name = name, isPermanent = true))
            loadMatros() // Обновляем список
        }
    }

    fun deleteMatros(matrosId: Int) {
        viewModelScope.launch {
            repository.deleteMatros(matrosId)
            loadMatros() // Обновляем список
        }
    }

    private fun logMatrosAction(matrosId: Int, actionType: String) {
        viewModelScope.launch {
            val currentShift = _currentShift.value ?: return@launch
            val matros =
                repository.getAllMatrosList().firstOrNull { it.id == matrosId } ?: return@launch

            val operation = Operation(
                type = "MATROS_ACTION",
                itemType = actionType,
                itemId = matrosId,
                shiftId = currentShift.id,
                startTime = TimeUtils.getCurrentTime(),
                status = "COMPLETED"
            )
            repository.saveOperation(operation)
        }
    }

    fun getMatrosName(matrosId: Int): String {
        return _allMatros.value.firstOrNull { it.id == matrosId }?.name ?: "Неизвестный"
    }

    fun addMatrosToShift(matrosId: Int) {
        viewModelScope.launch {
            _currentShift.value?.let { currentShift ->
                val updatedIds = if (currentShift.matrosIds.isEmpty()) {
                    matrosId.toString()
                } else {
                    "${currentShift.matrosIds},$matrosId"
                }
                repository.updateShift(currentShift.copy(matrosIds = updatedIds))
                _currentShift.value = currentShift.copy(matrosIds = updatedIds)
                logMatrosAction(matrosId, "JOIN_SHIFT") // Логируем действие

                _forceMatrosTab.value = false
            }
        }
    }

    fun removeMatrosFromShift(matrosId: Int) {
        viewModelScope.launch {
            _currentShift.value?.let { currentShift ->
                val updatedIds = currentShift.matrosIds.split(",")
                    .filter { it != matrosId.toString() }
                    .joinToString(",")
                repository.updateShift(currentShift.copy(matrosIds = updatedIds))
                _currentShift.value = currentShift.copy(matrosIds = updatedIds)

                // Проверяем, остались ли матросы на смене
                if (updatedIds.isEmpty()) {
                    _forceMatrosTab.value = true
                }

                logMatrosAction(matrosId, "LEAVE_SHIFT") // Логируем действие
            }
        }
    }

    fun openShift() {
        viewModelScope.launch {
            val shift = Shift(
                date = getCurrentDate(),
                status = true,
                matrosIds = ""
            )
            repository.saveShift(shift)
            _currentShift.value = shift
            _forceMatrosTab.value = true // Принудительно показываем вкладку "Матросы"
        }
    }

    fun closeShift() {
        viewModelScope.launch {
            _currentShift.value?.let { shift ->
                // 1. Генерация отчета
                val report = generateReport()

                // 2. Закрытие смены в БД
                repository.closeShift(shift)

                // 3. Очистка состояния
                _currentShift.value = null

                // 4. Отправка email (реализуется на уровне Activity)
                _emailReportEvent.tryEmit(report)
            }
        }
    }

    suspend fun getActiveOperations(): List<Operation> = withContext(Dispatchers.IO) {
        currentShift.value?.id?.let { shiftId ->
            repository.getActiveOperations(shiftId)
        } ?: emptyList()
    }

//    suspend fun cancelOperation(operationId: Int) {
//        repository.updateOperation(
//            repository.getOperationById(operationId)?.copy(status = "CANCELED")
//                ?: throw IllegalStateException("Operation not found")
//        )
//    }
//
//    suspend fun completeOperation(operationId: Int) {
//        repository.updateOperation(
//            repository.getOperationById(operationId)?.copy(status = "COMPLETED")
//                ?: throw IllegalStateException("Operation not found")
//        )
//    }

//    suspend fun completeOperation(operationId: Int) {
//        val operation = repository.getOperationById(operationId) ?: return
//
//        // 1. Обновляем основную операцию
//        repository.updateOperation(operation.copy(status = "COMPLETED"))
//
//        // 2. Обновляем все связанные продления
//        val extensions = repository.getExtensionsForOperation(operationId)
//        extensions.forEach { ext ->
//            repository.updateRentalExtension(ext.copy(status = "COMPLETED"))
//            Log.d(TAG_EXTENSION, "Marked extension ${ext.id} as COMPLETED")
//        }
//
//        // 3. Для групповых операций обновляем все операции группы
//        if (operation.groupId.isNotEmpty()) {
//            val groupOps = repository.getOperationsForGroup(operation.groupId)
//            groupOps.forEach { op ->
//                if (op.id != operationId) {
//                    repository.updateOperation(op.copy(status = "COMPLETED"))
//                    val groupExtensions = repository.getExtensionsForOperation(op.id)
//                    groupExtensions.forEach { ext ->
//                        repository.updateRentalExtension(ext.copy(status = "COMPLETED"))
//                    }
//                }
//            }
//        }
//    }

    suspend fun completeOperation(operationId: Int) {
        val operation = repository.getOperationById(operationId) ?: return
        repository.updateOperation(operation.copy(status = "COMPLETED"))

        // Обновляем все связанные продления
        val extensions = repository.getExtensionsForOperation(operationId)
        extensions.forEach { ext ->
            repository.updateRentalExtension(ext.copy(status = "COMPLETED"))
        }

        // Для групповых операций обновляем все операции группы и их продления
        if (operation.groupId.isNotEmpty()) {
            val groupOps = repository.getOperationsForGroup(operation.groupId)
            groupOps.forEach { op ->
                repository.updateOperation(op.copy(status = "COMPLETED"))
                val groupExtensions = repository.getExtensionsForOperation(op.id)
                groupExtensions.forEach { ext ->
                    repository.updateRentalExtension(ext.copy(status = "COMPLETED"))
                }
            }

            // Также обновляем групповые продления
            val groupExtensions = repository.getExtensionsForGroup(operation.groupId)
            groupExtensions.forEach { ext ->
                repository.updateRentalExtension(ext.copy(status = "COMPLETED"))
            }
        }
    }

    suspend fun cancelOperation(operationId: Int) {
        val operation = repository.getOperationById(operationId) ?: return

        // Аналогично для отмены
        repository.updateOperation(operation.copy(status = "CANCELED"))

        val extensions = repository.getExtensionsForOperation(operationId)
        extensions.forEach { ext ->
            repository.updateRentalExtension(ext.copy(status = "CANCELED"))
        }

        if (operation.groupId.isNotEmpty()) {
            val groupOps = repository.getOperationsForGroup(operation.groupId)
            groupOps.forEach { op ->
                if (op.id != operationId) {
                    repository.updateOperation(op.copy(status = "CANCELED"))
                    val groupExtensions = repository.getExtensionsForOperation(op.id)
                    groupExtensions.forEach { ext ->
                        repository.updateRentalExtension(ext.copy(status = "CANCELED"))
                    }
                }
            }
        }
    }

    // СУВЕНИРЫ И ШТРАФЫ

    // Добавляем списки сувениров и штрафов
    // 1. Загружаем сувениры из БД и объединяем с дефолтными
    private val defaultSouvenirNames =
        listOf("Шапка", "Брелок", "Верёвка", "Утка (продажа)", "Статуэтка")

    private val defaultSouvenirs = listOf(
        Souvenir(name = "Шапка", price = 500.0),
        Souvenir(name = "Брелок", price = 300.0),
        Souvenir(name = "Верёвка", price = 100.0),
        Souvenir(name = "Утка (продажа)", price = 2500.0),
        Souvenir(name = "Статуэтка", price = 500.0)
    )

    // Удаляем дублирование данных - используем только сувениры из базы
    val availableSouvenirs = repository.getAllSouvenirs()
        .map { souvenirs ->
            Log.d("SOUVENIR_FLOW", "Loaded ${souvenirs.size} souvenirs")
            souvenirs
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Упрощаем добавление нового сувенира
    fun addNewSouvenirType(name: String, price: Double) {
        viewModelScope.launch {
            try {
                if (!repository.souvenirExists(name)) {
                    repository.saveSouvenir(Souvenir(name = name, price = price))
                } else {
                    // Можно показать сообщение пользователю, что сувенир уже существует
                    // Например через Snackbar или Toast

                }
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }


    // Метод для получения цены по умолчанию
    private fun getDefaultPrice(name: String): Double {
        return when (name) {
            "Шапка" -> 500.0
            "Брелок" -> 300.0
            "Верёвка" -> 100.0
            "Утка (продажа)" -> 2500.0
            "Статуэтка" -> 500.0
            else -> 0.0
        }
    }

    fun deleteSouvenir(name: String) {
        viewModelScope.launch {
            repository.deleteSouvenir(name)
        }
    }


    // 3. Продажа сувенира (универсальная версия)
    suspend fun createSouvenirSale(
        itemName: String,
        isCash: Boolean,
        hasDiscount: Boolean
    ) {
        val souvenir = availableSouvenirs.value.firstOrNull { it.name == itemName } ?: return
        val currentShift = _currentShift.value ?: return

        val operation = Operation(
            type = "SALE",
            itemType = convertSouvenirNameToType(itemName),
            itemId = 0,
            shiftId = currentShift.id,
            startTime = getCurrentTime(),
            startTimestamp = System.currentTimeMillis(), // Добавляем
            endTime = "",
            status = "COMPLETED",
            isCash = isCash,
            hasDiscount = hasDiscount,
            price = if (hasDiscount) souvenir.price * 0.5 else souvenir.price
        )
        repository.saveOperation(operation)
    }

    private fun convertSouvenirNameToType(name: String): String {
        return when (name) {
            "Шапка" -> "HAT"
            "Брелок" -> "KEYCHAIN"
            "Верёвка" -> "ROPE"
            "Утка (продажа)" -> "DUCK_SALE"
            "Статуэтка" -> "STATUETTE"
            else -> name // Возвращаем оригинальное название для кастомных сувениров
        }
    }

    private val _availableFines = MutableStateFlow<List<String>>(
        listOf(
            "Штраф утка", "Штраф яхта"
        )
    )
    val availableFines: StateFlow<List<String>> = _availableFines.asStateFlow()

    // Метод для создания штрафа
    suspend fun createFine(fineType: String, isCash: Boolean) {
        val currentShift = _currentShift.value ?: return

        val operation = Operation(
            type = "FINE",
            itemType = when (fineType) {
                "Штраф утка" -> "FINE_DUCK"
                "Штраф яхта" -> "FINE_YACHT"
                else -> "FINE_OTHER"
            },
            itemId = 0,
            shiftId = currentShift.id,
            startTime = getCurrentTime(),
            startTimestamp = System.currentTimeMillis(), // Добавляем
            endTime = "",
            status = "COMPLETED",
            isCash = isCash,
            hasDiscount = false
        )
        repository.saveOperation(operation)
    }



    // Event для отправки email
    private val _emailReportEvent = MutableSharedFlow<String>()
    val emailReportEvent: SharedFlow<String> = _emailReportEvent

    // Добавляем новое состояние для ручного ввода оборудования
    private val _equipmentState = MutableStateFlow(EquipmentState())
    val equipmentState: StateFlow<EquipmentState> = _equipmentState

    // 1. Обновленный код для сохранения отчета
    data class ReportData(
        val financialData: String, // Основные финансовые показатели
        val equipmentData: EquipmentState // Данные об оборудовании
    ) {
        fun toJson(): String {
            return Json.encodeToString(this)
        }

        companion object {
            fun fromJson(json: String): ReportData {
                return Json.decodeFromString(json)
            }
        }
    }



    // 3. В MainViewModel добавляем функцию для загрузки отчетов
    fun getReportsForShift(shiftId: Int): Flow<List<Report>> {
        return repository.getReportsByShiftId(shiftId)
    }

    @Serializable
    data class EquipmentState(
        val workingYachts: String = "",
        val workingDucks: String = "",
        val brokenEquipment: String = "",
        val batteries: String = "",
        val isEdited: Boolean = false // Флаг, были ли изменены значения
    )

    // Добавляем data class для хранения информации о работе матроса
    private data class MatrosWorkInfo(
        val name: String,
        val joinTime: Long, // timestamp когда матрос пришел на смену
        val leaveTime: Long? // timestamp когда матрос ушел (null если еще на смене)
    )



    fun getRentalStats(): RentalStats {
        val allOps = _operations.value
        val (ducks, yachts) = allOps.partition { it.itemType == "DUCK" }

        return RentalStats(
            totalRentals = calculateEffectiveRentals(allOps),
            duckRentals = calculateEffectiveRentals(ducks),
            yachtRentals = calculateEffectiveRentals(yachts)
        )
    }

    private fun calculateEffectiveRentals(ops: List<Operation>): Int {
        return ops.sumOf { op ->
            when {
                // Основная аренда
                op.type == "RENTAL" && !op.isExtension -> 1 + op.extensionsCount
                // Операция продления
                op.isExtension -> 1
                // Другие операции не учитываем
                else -> 0
            }
        }
    }

    data class RentalStats(
        val totalRentals: Int,
        val duckRentals: Int,
        val yachtRentals: Int
    )

    // Новое вычисляемое свойство
    val completedOperations: StateFlow<List<Operation>>
        get() = operations.map { ops ->
            ops.filter { it.status == "COMPLETED" }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Обновленный расчет выручки
    fun calculateRevenues(): Pair<Double, Double> {
        val completed = completedOperations.value
        return Pair(
            completed.filter { it.itemType == "DUCK" }.sumOf { it.price ?: 350.0 },
            completed.filter { it.itemType == "YACHT" }.sumOf { it.price ?: 350.0 }
        )
    }

    // Обновляем функцию generateReport
    suspend fun generateReport(equipmentState: EquipmentState? = null): String = withContext(Dispatchers.IO) {

        Log.d(TAG_REPORT, "Starting report generation...")

        val shift = _currentShift.value ?: return@withContext ""
        // Получаем ВСЕ операции смены, а не только активные!
        val allOperations = repository.getOperationsForShift(shift.id).first()
        val allExtensions = repository.getAllExtensionsForShift(shift.id)

        Log.d(TAG_REPORT, "Found ${allOperations.size} operations and ${allExtensions.size} extensions")

        // 1. Собираем информацию о времени работы матросов
        val matrosWorkHistory = mutableListOf<MatrosWorkInfo>()

        // Получаем все события матросов за смену
        val matrosEvents = allOperations
            .filter { it.type == "MATROS_ACTION" }
            .sortedBy { it.startTimestamp }

        // Строим историю работы каждого матроса
        matrosEvents.groupBy { it.itemId }.forEach { (matrosId, events) ->
            var joinTime: Long? = null
            var leaveTime: Long? = null
            val name = getMatrosName(matrosId)

            events.forEach { event ->
                when (event.itemType) {
                    "JOIN_SHIFT" -> {
                        if (joinTime == null) {
                            joinTime = event.startTimestamp
                        }
                    }
                    "LEAVE_SHIFT" -> {
                        if (joinTime != null) {
                            matrosWorkHistory.add(
                                MatrosWorkInfo(
                                    name = name,
                                    joinTime = joinTime!!,
                                    leaveTime = event.startTimestamp
                                )
                            )
                            joinTime = null
                        }
                    }
                }
            }

            // Если матрос не ушел (все еще на смене)
            if (joinTime != null) {
                matrosWorkHistory.add(
                    MatrosWorkInfo(
                        name = name,
                        joinTime = joinTime!!,
                        leaveTime = null
                    )
                )
            }
        }

        // 1. Фильтруем операции по статусу "COMPLETED"
        val completedOps = allOperations.filter { it.status == "COMPLETED" }

        // 2. Получаем все завершенные аренды
        val completedRentals = allOperations
            .filter { it.type == "RENTAL" && it.status == "COMPLETED" }
            .sortedBy { it.startTimestamp }



        // ДОБАВЛЯЕМ ЛОГИРОВАНИЕ ЗДЕСЬ - ПЕРЕД РАСЧЕТОМ ЗАРПЛАТЫ
        Log.d("REPORT_DEBUG", "Все операции смены:")
        allOperations.forEach { op ->
            Log.d("REPORT_DEBUG",
                "ID: ${op.id}, Тип: ${op.type}/${op.itemType}, " +
                        "Статус: ${op.status}, Время: ${op.startTimestamp}, " +
                        "Цена: ${op.price}, Нал: ${op.isCash}, Скидка: ${op.hasDiscount}"
            )
        }

        // Логирование событий матросов
        Log.d("MATROS_DEBUG", "События матросов:")
        matrosWorkHistory.forEach {
            val leaveTimeStr = if (it.leaveTime == null) "еще на смене" else SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it.leaveTime))
            Log.d("MATROS_DEBUG",
                "Матрос ${it.name} работал с ${
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it.joinTime))
                } до $leaveTimeStr"
            )
        }

        // Логирование аренд
        Log.d("RENTAL_DEBUG", "Завершенные аренды:")
        completedRentals.forEach {
            Log.d("RENTAL_DEBUG",
                "${it.itemType} №${it.itemId} начата в ${
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it.startTimestamp))
                }, длительность: ${calculateDuration(it)} мин"
            )
        }


        // Логируем все операции
        allOperations.forEach { op ->
            Log.d(TAG_REPORT, """
            Operation ${op.id}: ${op.type} ${op.itemType} ${op.itemId}
            Status: ${op.status}, Price: ${op.price}
            Cash: ${op.isCash}, Discount: ${op.hasDiscount}
        """.trimIndent())
        }

        // Логируем все продления
        allExtensions.forEach { ext ->
            Log.d(TAG_REPORT, """
            Extension ${ext.id} for op ${ext.originalOperationId}
            Price: ${ext.price}, Cash: ${ext.isCash}, Discount: ${ext.hasDiscount}
        """.trimIndent())
        }

        // 3. Объединяем аренды и продления для расчетов
        val effectiveRentals = allOperations
            .filter { it.type == "RENTAL" && it.status == "COMPLETED" }
            .flatMap { op ->
                val extensions = allExtensions.filter { it.originalOperationId == op.id }
                listOf(op) + extensions.map { ext ->
                    Operation(
                        type = "RENTAL_EXTENSION",
                        itemType = op.itemType,
                        itemId = op.itemId,
                        shiftId = op.shiftId,
                        startTime = TimeUtils.formatTimeLong(ext.timestamp),
                        startTimestamp = ext.timestamp,
                        status = "COMPLETED",
                        isCash = ext.isCash,
                        hasDiscount = ext.hasDiscount,
                        price = ext.price,
//                        originalOperationId = op.id
                    ).also {
                        Log.d(TAG_REPORT, "Created virtual rental from extension: $it")
                    }
                }
            }

        // 4. Подсчет аренд (учитывая продления)
        val duckRentals = effectiveRentals.count { it.itemType == "DUCK" }
        val yachtRentals = effectiveRentals.count { it.itemType == "YACHT" }
        val totalRentals = duckRentals + yachtRentals

        // 5. Выручка по арендам (учитывая продления)
        val duckRevenue = effectiveRentals
            .filter { it.itemType == "DUCK" }
//            .sumOf {
//                if (it.hasDiscount) it.price * 0.5 else it.price
//            }
            .sumOf { op ->
                val price =
//                    if (op.hasDiscount) op.price * 0.5 else
                        op.price
                Log.d(TAG_REPORT, """
                Duck rental ${op.id}:
                Base price: ${op.price}, Discount: ${op.hasDiscount}, Cash: ${op.isCash}
                Status: ${op.status}
                Final price: $price
            """.trimIndent())
                price
            }

        Log.d(TAG_REPORT, "Total duck revenue: $duckRevenue")

        val yachtRevenue = effectiveRentals
            .filter { it.itemType == "YACHT" }
//            .sumOf {
//                if (it.hasDiscount) it.price * 0.5 else it.price
//            }
            .sumOf { op ->
                val price =
//                    if (op.hasDiscount) op.price * 0.5 else
                        op.price
                Log.d(TAG_REPORT, """
                Yacht rental ${op.id}:
                Base price: ${op.price}, Discount: ${op.hasDiscount}, Cash: ${op.isCash}
                Final price: $price
            """.trimIndent())
                price
            }

        Log.d(TAG_REPORT, "Total yacht revenue: $yachtRevenue")

        val rentalsRevenue = duckRevenue + yachtRevenue

//Зарплата матросов
        val matrosSalaries = mutableMapOf<String, Double>()
        effectiveRentals.forEach { rental ->
            val rentalTime = rental.startTimestamp
            val workingMatros = matrosWorkHistory.filter {
                it.joinTime <= rentalTime && (it.leaveTime == null || it.leaveTime >= rentalTime)
            }

            if (workingMatros.isNotEmpty()) {
                val salaryPerMatros = 75.0 / workingMatros.size
                workingMatros.forEach { matros ->
                    matrosSalaries[matros.name] = (matrosSalaries[matros.name] ?: 0.0) + salaryPerMatros
                }
            }
        }
        val totalSalary = matrosSalaries.values.sum()

        // 4. Сувениры
        val souvenirs = allOperations.filter { it.type == "SALE" && it.status == "COMPLETED" }
        val souvenirGroups = souvenirs.groupBy { it.itemType }
        val souvenirDetails = souvenirGroups.mapValues { (_, ops) ->
            ops.size to ops.sumOf {
                //if (it.hasDiscount) it.price * 0.5 else
                    it.price
            }
        }
        val totalSouvenirs = souvenirDetails.values.sumOf { it.second }

        // 5. Штрафы
        val fines = allOperations.filter { it.type == "FINE" && it.status == "COMPLETED" }
        val fineGroups = fines.groupBy { it.itemType }
        val fineDetails = fineGroups.mapValues { (_, ops) ->
            ops.size to ops.sumOf {
                when (it.itemType) {
                    "FINE_DUCK" -> 1000.0
                    "FINE_YACHT" -> 3000.0
                    else -> 0.0
                }
            }
        }
        val totalFines = fineDetails.values.sumOf { it.second }


        val extensionsCash = allExtensions
            .filter { it.isCash && it.status == "COMPLETED" }
            .sumOf { it.price }

        Log.d("REPORT_DEBUG", "Cash from extensions: $extensionsCash")


        val cashAmount = (allOperations.filter { it.isCash && it.status == "COMPLETED"} +
                allExtensions.filter { it.isCash && it.status == "COMPLETED"})
//                effectiveRentals.filter { it.isCash && it.status == "COMPLETED"})
            .sumOf {
                when {
                    it is Operation && it.type == "RENTAL" ->
//                        if (it.hasDiscount) it.price * 0.5 else
                            it.price
//                    it is Operation && it.type == "RENTAL_EXTENSION" -> it.price
                    it is Operation && it.type == "SALE" ->
//                        if (it.hasDiscount) it.price * 0.5 else
                            it.price
                    it is Operation && it.type == "FINE" ->
                        when (it.itemType) {
                            "FINE_DUCK" -> 1000.0
                            "FINE_YACHT" -> 3000.0
                            else -> 0.0
                        }
                    it is RentalExtension ->
//                        if (it.hasDiscount) it.price * 0.5 else
                            it.price
                    else -> 0.0
                }
            }


        // 8. Формируем отчёт
        val report = StringBuilder().apply {
            // Заголовок
            appendLine("ОТЧЕТ О СМЕНЕ")
            appendLine("Дата: ${shift.date}")
            appendLine()

            // Общая информация
            appendLine("=== ОБЩАЯ ИНФОРМАЦИЯ ===")
            appendLine("Общая выручка: ${(rentalsRevenue + totalSouvenirs + totalFines).toInt()}₽")
            appendLine("Наличные: ${cashAmount.toInt()}₽")
            appendLine()

            // Аренды
            appendLine("=== АРЕНДЫ ===")
            appendLine("Всего: $totalRentals")
            appendLine("• Утки: $duckRentals (${duckRevenue.toInt()}₽)")
            appendLine("• Яхты: $yachtRentals (${yachtRevenue.toInt()}₽)")
            appendLine()

            // Штрафы
            appendLine("=== ШТРАФЫ ===")
            appendLine("Всего: ${fines.size} (${totalFines.toInt()}₽)")
            fineDetails.forEach { (type, data) ->
                val typeName = when (type) {
                    "FINE_DUCK" -> "Штраф утка"
                    "FINE_YACHT" -> "Штраф яхта"
                    else -> type
                }
                appendLine("• $typeName: ${data.first} (${data.second.toInt()}₽)")
            }
            appendLine()

            // Сувениры
            appendLine("=== СУВЕНИРЫ ===")
            appendLine("Всего: ${souvenirs.size} (${totalSouvenirs.toInt()}₽)")
            souvenirDetails.forEach { (type, data) ->
                val typeName = when (type) {
                    "HAT" -> "Шапка"
                    "KEYCHAIN" -> "Брелок"
                    "ROPE" -> "Верёвка"
                    "DUCK_SALE" -> "Утка (продажа)"
                    "STATUETTE" -> "Статуэтка"
                    else -> type
                }
                appendLine("• $typeName: ${data.first} (${data.second.toInt()}₽)")
            }
            appendLine()

            // Зарплата
            appendLine("=== ЗАРПЛАТА ===")
            appendLine("Общая сумма: ${totalSalary.toInt()}₽")
            matrosSalaries.forEach { (name, salary) ->
                appendLine("• $name: ${salary}₽")
            }
        }

        return@withContext report.toString()
    }


    private fun calculateDuration(operation: Operation): Int {
        return if (operation.endTime.isNotEmpty() && operation.startTime.isNotEmpty()) {
            val start = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(operation.startTime)
            val end = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(operation.endTime)
            TimeUnit.MILLISECONDS.toMinutes(end.time - start.time).toInt()
        } else {
            0
        }
    }

    suspend fun saveFinalReport(
        equipmentState: EquipmentState,
        reportText: String,
        onComplete: () -> Unit
    ) {
        val fullReport = """
        $reportText
        
        ОБОРУДОВАНИЕ:
        Работает:
        • Яхты: ${equipmentState.workingYachts}
        • Утки: ${equipmentState.workingDucks}
        Не работает:
        • Утиль: ${equipmentState.brokenEquipment}
        Батарейки:
        • Работает: ${equipmentState.batteries}
    """.trimIndent()

        currentShift.value?.let { shift ->
            repository.saveReport(
                Report(
                    shiftId = shift.id,
                    data = fullReport // Сохраняем полный отчет с оборудованием
                )
            )
            _emailReportEvent.emit(fullReport) // Отправляем полный текст
            closeShift()
            onComplete()
        }
    }

    // Класс для хранения данных о зарплате
    private data class SalaryDetails(
        val totalSalary: Double,
        val matrosSalaries: Map<String, Double>
    )

    // Функция для расчета зарплат
    private suspend fun calculateSalaries(
        operations: List<Operation>,
        matrosEvents: List<Operation>
    ): SalaryDetails {
        // 1. Собираем информацию о времени работы матросов
        val matrosWorkPeriods = mutableMapOf<Int, MutableList<Pair<Long, Long?>>>()

        var currentMatrosIds = mutableSetOf<Int>()
        val allMatrosEvents = matrosEvents.sortedBy { it.startTimestamp }

        allMatrosEvents.forEach { event ->
            when (event.itemType) {
                "JOIN_SHIFT" -> {
                    currentMatrosIds.add(event.itemId)
                    matrosWorkPeriods.getOrPut(event.itemId) { mutableListOf() }
                        .add(event.startTimestamp to null)
                }
                "LEAVE_SHIFT" -> {
                    matrosWorkPeriods[event.itemId]?.lastOrNull()?.let { lastPeriod ->
                        if (lastPeriod.second == null) {
                            val index = matrosWorkPeriods[event.itemId]!!.size - 1
                            matrosWorkPeriods[event.itemId]!![index] = lastPeriod.first to event.startTimestamp
                        }
                    }
                    currentMatrosIds.remove(event.itemId)
                }
            }
        }

        // Для матросов, которые не ушли (период не закрыт)
        matrosWorkPeriods.forEach { (matrosId, periods) ->
            periods.forEachIndexed { index, period ->
                if (period.second == null) {
                    periods[index] = period.first to System.currentTimeMillis()
                }
            }
        }

        // 2. Рассчитываем зарплату для каждого матроса
        val matrosSalaries = mutableMapOf<String, Double>()
        val rentalOperations = operations
            .filter { it.type == "RENTAL" && it.status == "COMPLETED" }
            .sortedBy { it.startTimestamp }

        rentalOperations.forEach { operation ->
            val operationTime = operation.startTimestamp
            val activeMatrosCount = matrosWorkPeriods.count { (_, periods) ->
                periods.any { (start, end) ->
                    operationTime >= start && operationTime <= (end ?: System.currentTimeMillis())
                }
            }

            if (activeMatrosCount > 0) {
                val salaryPerMatros = 75.0 / activeMatrosCount
                matrosWorkPeriods.forEach { (matrosId, periods) ->
                    if (periods.any { (start, end) ->
                            operationTime >= start && operationTime <= (end ?: System.currentTimeMillis())
                        }) {
                        val matrosName = getMatrosName(matrosId)
                        matrosSalaries[matrosName] = (matrosSalaries[matrosName] ?: 0.0) + salaryPerMatros
                    }
                }
            }
        }

        return SalaryDetails(
            totalSalary = matrosSalaries.values.sum(),
            matrosSalaries = matrosSalaries
        )
    }

    // Функция для обновления состояния оборудования
    fun updateEquipmentState(
        workingYachts: String? = null,
        workingDucks: String? = null,
        brokenEquipment: String? = null,
        batteries: String? = null
    ) {
        val current = _equipmentState.value
        _equipmentState.value = current.copy(
            workingYachts = workingYachts ?: current.workingYachts,
            workingDucks = workingDucks ?: current.workingDucks,
            brokenEquipment = brokenEquipment ?: current.brokenEquipment,
            batteries = batteries ?: current.batteries
        )
    }

    sealed class MatrosState {
        object Loading : MatrosState()
        data class Error(val message: String) : MatrosState()
        data class Success(val matrosList: List<Matros>) : MatrosState()
    }

    //календарь

    fun getAvailableReportDates(): Flow<List<String>> = repository.getReportDates()

    suspend fun getReportByDate(date: String): String? {
        return repository.getReportByDate(date)?.data
    }
}