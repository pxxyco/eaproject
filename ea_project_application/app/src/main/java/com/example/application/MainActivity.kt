package com.example.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.application.network.TokenStorage
import com.example.application.ui.theme.ApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenStorage.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            ApplicationTheme {
                MainContainer()
            }
        }
    }
}