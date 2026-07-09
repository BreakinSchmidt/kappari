package com.breakinschmidt.kappariwear.ui

import android.app.RemoteInput
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import com.breakinschmidt.kappariwear.data.AuthManager
import com.breakinschmidt.kappariwear.network.PaprikaApiClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(authManager: AuthManager) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val emailLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val results: Bundle? = RemoteInput.getResultsFromIntent(result.data)
        val input = results?.getCharSequence("email")
        if (input != null) {
            email = input.toString()
        }
    }

    val passwordLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val results: Bundle? = RemoteInput.getResultsFromIntent(result.data)
        val input = results?.getCharSequence("password")
        if (input != null) {
            password = input.toString()
        }
    }

    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Paprika Login", textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
        }
        
        item {
            Chip(
                onClick = {
                    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                    val remoteInputs = listOf(RemoteInput.Builder("email").setLabel("Email").build())
                    RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                    emailLauncher.launch(intent)
                },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(if (email.isEmpty()) "Enter Email" else email) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            Chip(
                onClick = {
                    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                    val remoteInputs = listOf(RemoteInput.Builder("password").setLabel("Password").build())
                    RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                    passwordLauncher.launch(intent)
                },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(if (password.isEmpty()) "Enter Password" else "********") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            if (errorMsg != null) {
                Text(errorMsg!!, color = androidx.wear.compose.material.MaterialTheme.colors.error)
            }
        }
        
        item {
            Button(
                onClick = {
                    isLoading = true
                    errorMsg = null
                    coroutineScope.launch {
                        try {
                            val response = PaprikaApiClient.api.login(email, password)
                            if (response.result != null) {
                                authManager.saveCredentials(email, password)
                                authManager.saveToken(response.result.token)
                            } else {
                                errorMsg = response.error?.message ?: "Login failed"
                            }
                        } catch (e: Exception) {
                            errorMsg = e.message ?: "Network error"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
            ) {
                Text(if (isLoading) "..." else "Login")
            }
        }
    }
}
