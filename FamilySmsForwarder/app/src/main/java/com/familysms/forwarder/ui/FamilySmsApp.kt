package com.familysms.forwarder.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familysms.forwarder.ui.components.AddSenderDialog
import com.familysms.forwarder.ui.screens.DashboardScreen
import com.familysms.forwarder.ui.screens.SendersScreen
import com.familysms.forwarder.ui.screens.SettingsScreen

enum class AppTab(
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    SENDERS("Senders", Icons.Default.People),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilySmsApp(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var showAddSenderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Family SMS Forwarder") }
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == AppTab.SENDERS) {
                FloatingActionButton(onClick = { showAddSenderDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add sender")
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            AppTab.DASHBOARD -> {
                DashboardScreen(
                    uiState = uiState,
                    onToggleMaster = viewModel::setMasterEnabled,
                    onRequestPermissions = onRequestPermissions,
                    onOpenAppSettings = onOpenAppSettings,
                    onOpenBatterySettings = onOpenBatterySettings,
                    onNavigateToSenders = { currentTab = AppTab.SENDERS },
                    onNavigateToSettings = { currentTab = AppTab.SETTINGS },
                    onDeleteLog = viewModel::deleteLog,
                    onClearAllLogs = viewModel::clearAllLogs,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppTab.SENDERS -> {
                SendersScreen(
                    rules = uiState.senderRules,
                    onToggleRule = viewModel::setSenderRuleEnabled,
                    onDeleteRule = viewModel::deleteSenderRule,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppTab.SETTINGS -> {
                SettingsScreen(
                    settings = uiState.settings,
                    batteryOptimizationIgnored = uiState.permissionStatus.batteryOptimizationIgnored,
                    onToggleMaster = viewModel::setMasterEnabled,
                    onSaveDestinationNumber = viewModel::saveDestinationNumber,
                    onToggleIncludeTimestamp = viewModel::setIncludeTimestamp,
                    onSelectDedupeWindow = viewModel::setDedupeWindowSeconds,
                    onToggleNotifyOnError = viewModel::setNotifyOnError,
                    onToggleNotifyOnForwarded = viewModel::setNotifyOnForwarded,
                    onOpenBatterySettings = onOpenBatterySettings,
                    onOpenAppSettings = onOpenAppSettings,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        if (showAddSenderDialog) {
            AddSenderDialog(
                onDismiss = { showAddSenderDialog = false },
                onConfirm = { pattern, label, matchType ->
                    viewModel.addSenderRule(pattern, label, matchType)
                    showAddSenderDialog = false
                }
            )
        }
    }
}
