package com.example.captainsclub2.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.captainsclub2.ui.screens.MainScreen
import com.example.captainsclub2.ui.screens.OpenShiftScreen
import com.example.captainsclub2.ui.viewmodels.MainViewModel
import com.example.captainsclub2.utils.EmailUtils

// Изменяем навигацию после открытия смены
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = hiltViewModel()
    val currentShift by viewModel.currentShift.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (currentShift == null) "open_shift" else "main"
    ) {
        composable("open_shift") {
            OpenShiftScreen(
                onShiftOpened = { viewModel.openShift() }
            )
        }
        composable("main") {
            MainScreen(
                onCloseShift = { viewModel.closeShift() }
            )
        }
    }
}