package com.example.captainsclub2.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.captainsclub2.data.models.Report
import com.example.captainsclub2.ui.viewmodels.MainViewModel
import com.example.captainsclub2.repository.ShiftRepository
import com.example.captainsclub2.ui.components.CalendarDialog
import com.example.captainsclub2.ui.components.ReportViewDialog
import com.example.captainsclub2.utils.EmailUtils
import com.example.captainsclub2.utils.TimeUtils
import com.example.captainsclub2.utils.TimeUtils.getCurrentDate
import com.example.captainsclub2.utils.TimeUtils.getCurrentTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onCloseShift: () -> Unit
) {
    val context = LocalContext.current

    var showCalendarDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<String?>(null) }
    val availableDates by viewModel.getAvailableReportDates().collectAsState(emptyList())

    // Обработка события отправки email
    LaunchedEffect(Unit) {
        viewModel.emailReportEvent.collect { reportText ->
            EmailUtils.sendReportEmail(context, reportText)

        }
    }

    var currentTime by remember { mutableStateOf(TimeUtils.getCurrentTime()) }


    LaunchedEffect(Unit) {
        while (true) {
            delay(1000 - System.currentTimeMillis() % 1000) // Синхронизация с началом секунды
            currentTime = TimeUtils.getCurrentTime()
        }
    }

    val forceMatrosTab by viewModel.forceMatrosTab.collectAsState()
    val tabs = listOf("Прокат", "Отчет", "Матросы")

    //var selectedTabIndex by remember { mutableIntStateOf(2) }
    var selectedTabIndex by remember { mutableIntStateOf(if (forceMatrosTab) 2 else 0) }

   // var showConfirmDialog by remember { mutableStateOf(false) }
   // var reportText by remember { mutableStateOf("") }

   // val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Автоматическое переключение на вкладку "Матросы" при отсутствии матросов
    LaunchedEffect(viewModel.matrosOnShift) {
        if (viewModel.matrosOnShift.value.isEmpty()) {
            selectedTabIndex = 2
        }
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showEquipmentDialog by remember { mutableStateOf(false) }
    var reportText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val equipmentState by viewModel.equipmentState.collectAsState()


    if (showConfirmDialog) {
        var workingYachts by remember { mutableStateOf(viewModel.equipmentState.value.workingYachts) }
        var workingDucks by remember { mutableStateOf(viewModel.equipmentState.value.workingDucks) }
        var brokenEquipment by remember { mutableStateOf(viewModel.equipmentState.value.brokenEquipment) }
        var batteries by remember { mutableStateOf(viewModel.equipmentState.value.batteries) }

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    "Финальный отчет",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp)
                ) {
                    // Новое отображение отчета по строкам
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        reportText.split("\n").forEach { line ->
                            if (line.isNotBlank()) {
                                val style = when {
                                    line.startsWith("===") -> MaterialTheme.typography.titleMedium
                                    line.startsWith("ОТЧЕТ") -> MaterialTheme.typography.titleLarge
                                    else -> MaterialTheme.typography.bodyMedium
                                }

                                Text(
                                    text = line,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = if (line.startsWith("===")) TextAlign.Center else TextAlign.Start,
                                    style = style.copy(lineHeight = 18.sp)
                                )
                            }
                        }
                    }

                    Divider()

                    // Секция для ручного ввода оборудования
                    Text("Оборудование:", style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Работает:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Яхты:", modifier = Modifier.width(80.dp))
                        OutlinedTextField(
                            value = workingYachts,
                            onValueChange = { workingYachts = it },
                            modifier = Modifier.width(100.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Утки:", modifier = Modifier.width(80.dp))
                        OutlinedTextField(
                            value = workingDucks,
                            onValueChange = { workingDucks = it },
                            modifier = Modifier.width(100.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Не работает:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Утиль:", modifier = Modifier.width(80.dp))
                        OutlinedTextField(
                            value = brokenEquipment,
                            onValueChange = { brokenEquipment = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Батарейки:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Работает:", modifier = Modifier.width(80.dp))
                        OutlinedTextField(
                            value = batteries,
                            onValueChange = { batteries = it },
                            modifier = Modifier.width(100.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            // Используем метод saveFinalReport из ViewModel
                            viewModel.saveFinalReport(
                                equipmentState = MainViewModel.EquipmentState(
                                    workingYachts = workingYachts,
                                    workingDucks = workingDucks,
                                    brokenEquipment = brokenEquipment,
                                    batteries = batteries
                                ),
                                reportText = reportText,
                                onComplete = onCloseShift
                            )
                            showConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                ) {
                    Text("Сохранить и завершить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showCalendarDialog) {
        CalendarDialog(
            availableDates = availableDates,
            onDateSelected = { date ->
                viewModel.viewModelScope.launch {
                    Log.d("REPORT_LOAD", "Attempting to load report for date: $date")
                    try {
                        selectedReport = viewModel.getReportByDate(date)?.also { report ->
                            Log.d("REPORT_LOAD", "Successfully loaded report for date: $date. Content preview: ${report.take(100)}...")
                        }
                        showCalendarDialog = false
                        showReportDialog = true
                    } catch (e: Exception) {
                        Log.e("REPORT_LOAD", "Failed to load report for date $date", e)
                        // Можно показать пользователю сообщение об ошибке
                    }
                }
            },
            onDismiss = { showCalendarDialog = false }
        )
    }

    if (showReportDialog && selectedReport != null) {
        ReportViewDialog(
            reportText = selectedReport!!,
            onExportClick = {
                if (selectedReport!!.isNotBlank()) {
                    EmailUtils.sendArchiveReport(context, selectedReport!!)
                } else {
                    Toast.makeText(context, "Отчет пустой", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showReportDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.LightGray.copy(alpha = 0.2f), // Светло-серый с небольшой прозрачностью
                contentColor = Color.Black, // Черный цвет текста для контраста
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF0D47A1) // Синий цвет индикатора активной вкладки
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            if (index != 2 && viewModel.matrosOnShift.value.isEmpty()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "На смене нет ни одного матроса! Присоединитесь к смене!"
                                    )
                                }
                            } else {
                                selectedTabIndex = index
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                //color = if (selectedTabIndex == index) Color.Black else Color.Gray // Черный для активной, серый для неактивных
                                color = Color.Black
                            )
                        }
                    )
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFFF5F5F5),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.height(64.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            )
            {
                Text(
                    text = "Смена ${viewModel.currentShift.value?.date ?: ""}",
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .clickable { showCalendarDialog = true },
                    fontSize = 16.sp, // Увеличиваем шрифт даты
                    color = Color.DarkGray // Темно-серый цвет текста
                )
                Spacer(modifier = Modifier.weight(1f))


                Text(
                    text = TimeUtils.formatTimeForDisplay(currentTime),
                    modifier = Modifier
                        .padding(horizontal = 16.dp),

                    fontSize = 40.sp, // Увеличиваем шрифт времени (больше, чем у даты)
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray // Темно-серый цвет текста
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        scope.launch {
                            reportText = viewModel.generateReport()
                            showConfirmDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                ) {
                    Text("Завершить смену")
                }
            }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTabIndex) {
                0 -> RentalScreen(viewModel)
                1 -> ReportScreen(viewModel)
                2 -> MatrosScreen(viewModel)
            }
        }
    }
}