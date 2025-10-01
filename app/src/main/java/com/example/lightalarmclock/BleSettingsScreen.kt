package com.example.lightalarmclock

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BleSettingsScreen(
    viewModel: BleSettingsViewModel = viewModel()
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Bluetooth Settings", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = viewModel.deviceAddress.value,
            onValueChange = { viewModel.deviceAddress.value = it },
            label = { Text("Device Address") }
        )
        OutlinedTextField(
            value = viewModel.serviceUuid.value,
            onValueChange = { viewModel.serviceUuid.value = it },
            label = { Text("Service UUID") }
        )
        OutlinedTextField(
            value = viewModel.message.value,
            onValueChange = { viewModel.message.value = it },
            label = { Text("Message") }
        )

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Bluetooth Settings")
        }

        Button(onClick = {viewModel.sendMessage(context) }) { Text("Test Connection") }
        Text(viewModel.connectionStatus.value, color = if (viewModel.connectionStatus.value.contains("Success")) Color.Green else Color.Red)


        Button(onClick = { viewModel.sendMessage(context) }) { Text("Send Message") }
        Text(viewModel.messageStatus.value, color = if (viewModel.messageStatus.value.contains("Sent")) Color.Green else Color.Red)
    }
}
