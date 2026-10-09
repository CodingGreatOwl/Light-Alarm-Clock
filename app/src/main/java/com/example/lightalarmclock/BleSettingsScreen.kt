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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment

@Composable
fun BleSettingsScreen(
    viewModel: BleSettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Bluetooth Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Device Address
        OutlinedTextField(
            value = viewModel.deviceAddress.value,
            onValueChange = {
                viewModel.deviceAddress.value = it
                viewModel.updateSettings() // Auto-save on change
            },
            label = { Text("Device Address") },
            placeholder = { Text("XX:XX:XX:XX:XX:XX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Service UUID
        OutlinedTextField(
            value = viewModel.serviceUuid.value,
            onValueChange = {
                viewModel.serviceUuid.value = it
                viewModel.updateSettings() // Auto-save on change
            },
            label = { Text("Service UUID") },
            placeholder = { Text("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Characteristic UUID
        OutlinedTextField(
            value = viewModel.characteristicUuid.value,
            onValueChange = {
                viewModel.characteristicUuid.value = it
                viewModel.updateSettings() // Auto-save on change
            },
            label = { Text("Characteristic UUID") },
            placeholder = { Text("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Message
        OutlinedTextField(
            value = viewModel.message.value,
            onValueChange = {
                viewModel.message.value = it
                viewModel.updateSettings() // Auto-save on change
            },
            label = { Text("Message") },
            placeholder = { Text("Enter message to send") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Open System Bluetooth Settings
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Bluetooth Settings")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Test Connection Button
        Button(
            onClick = { viewModel.testConnection(context) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("Test Connection")
        }

        // Connection Status
        if (viewModel.connectionStatus.value.isNotEmpty()) {
            Text(
                text = viewModel.connectionStatus.value,
                color = if (viewModel.connectionStatus.value.contains("Success", ignoreCase = true) ||
                    viewModel.connectionStatus.value.contains("successful", ignoreCase = true))
                    Color.Green else Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Send Message Button
        Button(
            onClick = { viewModel.sendMessage(context) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Send Test Message")
        }

        // Message Status
        if (viewModel.messageStatus.value.isNotEmpty()) {
            Text(
                text = viewModel.messageStatus.value,
                color = if (viewModel.messageStatus.value.contains("Sent", ignoreCase = true) ||
                    viewModel.messageStatus.value.contains("sent", ignoreCase = true))
                    Color.Green else Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}