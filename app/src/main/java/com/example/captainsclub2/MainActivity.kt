package com.example.captainsclub2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.captainsclub2.navigation.AppNavGraph
import com.example.captainsclub2.ui.theme.CaptainsClub2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // Обязательно для Hilt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContent {
                CaptainsClub2Theme {
                    AppNavGraph()
                }
            }
        } catch (e: Exception) {
            Log.e("CRASH", "App crashed", e)
            throw e
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CaptainsClub2Theme {
        AppNavGraph()
    }
}