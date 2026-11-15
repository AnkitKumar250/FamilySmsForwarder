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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.familysms.forwarder.data.local.ForwardLogEntity
import com.familysms.forwarder.domain.ForwardStatus
import com.familysms.forwarder.ui.MainUiState
import com.familysms.forwarder.ui.components.StatusBadge
import com.familysms.forwarder.ui.theme.ErrorRed
import com.familysms.forwarder.ui.theme.ErrorRedContainer
import com.familysms.forwarder.ui.theme.SuccessGreen
import com.familysms.forwarder.ui.theme.SuccessGreenContainer
import com.familysms.forwarder.ui.theme.WarningAmber
import com.familysms.forwarder.ui.theme.WarningAmberContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    onToggleMaster: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onNavigateToSenders: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDeleteLog: (Long) -> Unit,
    onClearAllLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ForwardingStatusCard(
                uiState = uiState,
                onToggleMaster = onToggleMaster,
                onNavigateToSenders = onNavigateToSenders,
                onNavigateToSettings = onNavigateToSettings
            )
        }

        item {
            PermissionsChecklistCard(
                uiState = uiState,
                onRequestPermissions = onRequestPermissions,
                onOpenAppSettings = onOpenAppSettings,
                onOpenBatterySettings = onOpenBatterySettings
            )
        }

        if (uiState.recentErrors.isNotEmpty()) {
            item {
                RecentErrorsCard(errors = uiState.recentErrors)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Forwarding History",
                    style = MaterialTheme.typography.titleMedium
                )
                if (uiState.recentLogs.isNotEmpty()) {
                    TextButton(onClick = onClearAllLogs) {
                        Text("Clear History")
                    }
                }
            }
        }

        if (uiState.recentLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No SMS messages forwarded yet",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Incoming SMS matching your active sender rules will appear here with real-time carrier send and delivery tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.recentLogs, key = { it.id }) { log ->
                ForwardLogCard(
                    log = log,
                    onDelete = { onDeleteLog(log.id) }
                )
            }
        }
    }
}

@Composable
private fun ForwardingStatusCard(
    uiState: MainUiState,
    onToggleMaster: (Boolean) -> Unit,
    onNavigateToSenders: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val isActive = uiState.isReadyToForward
    val isEnabled = uiState.settings.isMasterEnabled

    val (containerColor, iconTint, titleText, subtitleText) = when {
        isActive -> StatusVisual(
            containerColor = SuccessGreenContainer,
            iconTint = SuccessGreen,
            title = "Forwarding Active",
            subtitle = "Monitoring ${uiState.enabledRuleCount} sender rule(s) -> ${uiState.settings.destinationNumber}"
        )
        isEnabled -> StatusVisual(
            containerColor = WarningAmberContainer,
            iconTint = WarningAmber,
            title = "Setup Incomplete",
            subtitle = buildMissingSetupText(uiState)
        )
        else -> StatusVisual(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            title = "Forwarding Paused",
            subtitle = "Turn on the master switch to forward matching incoming SMS."
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            isActive -> Icons.Default.CheckCircle
                            isEnabled -> Icons.Default.WarningAmber
                            else -> Icons.Default.PauseCircleOutline
                        },
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleMaster
                )
            }

            if (!uiState.isDestinationValid || uiState.enabledRuleCount == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!uiState.isDestinationValid) {
                        OutlinedButton(onClick = onNavigateToSettings) {
                            Text("Set Destination")
                        }
                    }
                    if (uiState.enabledRuleCount == 0) {
                        OutlinedButton(onClick = onNavigateToSenders) {
                            Text("Add Senders")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsChecklistCard(
    uiState: MainUiState,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val perms = uiState.permissionStatus
    val allRuntimeGranted = perms.allRequiredSmsPermissionsGranted && perms.postNotificationsGranted

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
                text = "Permissions & Background Health",
                style = MaterialTheme.typography.titleMedium
            )

            PermissionStatusRow(
                label = "Receive SMS (RECEIVE_SMS)",
                granted = perms.receiveSmsGranted,
                required = true
            )
            PermissionStatusRow(
                label = "Send SMS (SEND_SMS)",
                granted = perms.sendSmsGranted,
                required = true
            )
            PermissionStatusRow(
                label = "Status Notifications (POST_NOTIFICATIONS)",
                granted = perms.postNotificationsGranted,
                required = false
            )
            PermissionStatusRow(
                label = "Unrestricted Battery (Doze Exemption)",
                granted = perms.batteryOptimizationIgnored,
                required = false
            )

            if (!allRuntimeGranted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRequestPermissions) {
                        Text("Grant Permissions")
                    }
                    OutlinedButton(onClick = onOpenAppSettings) {
                        Text("App Info")
                    }
                }
                Text(
                    text = "Android 13–16 sideload note: If SMS permissions appear greyed out, open App Info -> 3-dot menu -> Allow restricted settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!perms.batteryOptimizationIgnored) {
                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disable Battery Optimization for Reliable Forwarding")
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean,
    required: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = when {
                granted -> "Granted"
                required -> "Required"
                else -> "Recommended"
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = when {
                granted -> SuccessGreen
                required -> ErrorRed
                else -> WarningAmber
            }
        )
    }
}

@Composable
private fun RecentErrorsCard(errors: List<ForwardLogEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ErrorRedContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = ErrorRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Forwarding Errors (${errors.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = ErrorRed
                )
            }
            errors.take(3).forEach { err ->
                Text(
                    text = "• ${err.originalSender}: ${err.errorMessage ?: "Unknown failure"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed
                )
            }
        }
    }
}

@Composable
private fun ForwardLogCard(
    log: ForwardLogEntity,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()) }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${log.originalSender} -> ${log.destinationNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Rule: ${log.matchedRuleLabel} • ${dateFormat.format(Date(log.receivedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = log.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = log.messageBody,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (log.totalParts > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Multipart SMS: ${log.sentPartsCount}/${log.totalParts} sent, ${log.deliveredPartsCount}/${log.totalParts} delivered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!log.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = log.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (log.status == ForwardStatus.FAILED) ErrorRed else WarningAmber
                )
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Updated: ${dateFormat.format(Date(log.updatedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete log entry"
                        )
                    }
                }
            }
        }
    }
}

private data class StatusVisual(
    val containerColor: androidx.compose.ui.graphics.Color,
    val iconTint: androidx.compose.ui.graphics.Color,
    val title: String,
    val subtitle: String
)

private fun buildMissingSetupText(uiState: MainUiState): String {
    val missing = mutableListOf<String>()
    if (!uiState.permissionStatus.allRequiredSmsPermissionsGranted) missing.add("SMS permissions")
    if (!uiState.isDestinationValid) missing.add("destination number")
    if (uiState.enabledRuleCount == 0) missing.add("at least 1 active sender")
    return if (missing.isEmpty()) "Ready" else "Missing: ${missing.joinToString(", ")}"
}
