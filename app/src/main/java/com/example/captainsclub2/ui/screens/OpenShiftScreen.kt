package com.example.captainsclub2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.captainsclub2.ui.viewmodels.MainViewModel
import com.example.captainsclub2.utils.TimeUtils
import com.example.captainsclub2.utils.TimeUtils.getCurrentDate

@Composable
fun OpenShiftScreen(
    onShiftOpened: () -> Unit // Принимаем колбэк
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "Смена ${TimeUtils.getCurrentDate()}", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onShiftOpened, // Передаём колбэк в кнопку
            modifier = Modifier.padding(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
        ) {
            Text("Открыть смену")
        }
    }
}