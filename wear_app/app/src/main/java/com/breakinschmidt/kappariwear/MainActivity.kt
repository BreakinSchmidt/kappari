package com.breakinschmidt.kappariwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.breakinschmidt.kappariwear.data.AuthManager
import com.breakinschmidt.kappariwear.ui.GroceryListScreen
import com.breakinschmidt.kappariwear.ui.LoginScreen

class MainActivity : ComponentActivity() {
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)
        com.breakinschmidt.kappariwear.network.PaprikaApiClient.initialize(authManager)

        setContent {
            MaterialTheme {
                Scaffold(
                    timeText = { TimeText() }
                ) {
                    val token by authManager.jwtToken.collectAsState(initial = null)
                    
                    if (token == null) {
                        LoginScreen(authManager)
                    } else {
                        GroceryListScreen(token!!, authManager)
                    }
                }
            }
        }
    }
}
