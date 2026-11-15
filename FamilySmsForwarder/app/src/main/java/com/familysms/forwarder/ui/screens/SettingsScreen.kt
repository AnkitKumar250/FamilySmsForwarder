package com.familysms.forwarder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.familysms.forwarder.data.preferences.AppSettings
import com.familysms.forwarder.domain.PhoneNumberNormalizer

@Composable
fun SettingsScreen(
    settings: AppSettings,
    batteryOptimizationIgnored: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    onSaveDestinationNumber: (String) -> Unit,
    onToggleIncludeTimestamp: (Boolean) -> Unit,
    onSelectDedupeWindow: (Int) -> Unit,
    onToggleNotifyOnError: (Boolean) -> Unit,
    onToggleNotifyOnForwarded: (Boolean) -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var destinationInput by remember { mutableStateOf(settings.destinationNumber) }
    var destinationError by remember { mutableStateOf<String?>(null) }
    var savedBanner by remember { mutableStateOf(false) }

    LaunchedEffect(settings.destinationNumber) {
        if (destinationInput.isEmpty() && settings.destinationNumber.isNotEmpty()) {
            destinationInput = settings.destinationNumber
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Master Forwarding Switch",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Enable or pause automatic SMS forwarding globally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isMasterEnabled,
                        onCheckedChange = onToggleMaster
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Family Destination Number",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "All matched SMS messages will be forwarded to this mobile number using your SIM card via SmsManager.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = destinationInput,
                        onValueChange = {
                            destinationInput = it
                            destinationError = null
                            savedBanner = false
                        },
                        label = { Text("Destination Mobile Number") },
                        placeholder = { Text("e.g. +15559876543") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        isError = destinationError != null,
                        supportingText = {
                            when {
                                destinationError != null -> Text(destinationError!!)
                                savedBanner -> Text("Saved locally.")
                                else -> Text("Include country code (E.164 format) for best carrier reliability.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val trimmed = destinationInput.trim()
                            if (!PhoneNumberNormalizer.isValidDestinationNumber(trimmed)) {
                                destinationError = "Enter a valid mobile number (7–15 digits)."
                            } else {
                                onSaveDestinationNumber(trimmed)
                                savedBanner = true
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Number")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Forwarding Options",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SettingSwitchRow(
                        title = "Include Received Timestamp",
                        subtitle = "Appends date & time to the forwarded header.",
                        checked = settings.includeTimestamp,
                        onCheckedChange = onToggleIncludeTimestamp
                    )

                    SettingSwitchRow(
                        title = "Notify on Forwarding Errors",
                        subtitle = "Show a local notification if carrier send fails.",
                        checked = settings.notifyOnError,
                        onCheckedChange = onToggleNotifyOnError
                    )

                    SettingSwitchRow(
                        title = "Notify on Successful Forward",
                        subtitle = "Show a local notification for every forwarded SMS.",
                        checked = settings.notifyOnForwarded,
                        onCheckedChange = onToggleNotifyOnForwarded
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Duplicate Protection Window",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Suppresses identical SMS messages from the same sender within this interval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 60, 120, 300).forEach { seconds ->
                            FilterChip(
                                selected = settings.dedupeWindowSeconds == seconds,
                                onClick = { onSelectDedupeWindow(seconds) },
                                label = { Text("${seconds}s") }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Android 14, 15 & 16 Reliability Guide",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "• Battery Optimization: ${if (batteryOptimizationIgnored) "Unrestricted (Recommended)" else "Optimized (May delay background forwarding)"}\n" +
                            "• Never Force-Stop: Swiping the app from Recents is safe, but Force Stop in Android Settings blocks SMS_RECEIVED broadcasts until reopened.\n" +
                            "• Restricted Settings (Android 13–16): Sideloaded APKs require App Info -> 3-dot menu -> Allow restricted settings if SMS permissions are blocked.\n" +
                            "• OEM Auto-Start: On Xiaomi, Oppo, Vivo, or Samsung One UI, also set App Battery Usage to Unrestricted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenBatterySettings) {
                            Text("Battery Settings")
                        }
                        OutlinedButton(onClick = onOpenAppSettings) {
                            Text("App Info")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
