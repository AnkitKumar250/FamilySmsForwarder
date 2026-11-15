package com.familysms.forwarder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familysms.forwarder.FamilySmsApplication
import com.familysms.forwarder.data.local.ForwardLogEntity
import com.familysms.forwarder.data.local.SenderRuleEntity
import com.familysms.forwarder.data.preferences.AppSettings
import com.familysms.forwarder.domain.MatchType
import com.familysms.forwarder.domain.PhoneNumberNormalizer
import com.familysms.forwarder.util.PermissionHelper
import com.familysms.forwarder.util.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val senderRules: List<SenderRuleEntity> = emptyList(),
    val recentLogs: List<ForwardLogEntity> = emptyList(),
    val recentErrors: List<ForwardLogEntity> = emptyList(),
    val permissionStatus: PermissionStatus = PermissionStatus(
        receiveSmsGranted = false,
        sendSmsGranted = false,
        postNotificationsGranted = false,
        batteryOptimizationIgnored = false
    )
) {
    val enabledRuleCount: Int
        get() = senderRules.count { it.isEnabled }

    val isDestinationValid: Boolean
        get() = PhoneNumberNormalizer.isValidDestinationNumber(settings.destinationNumber)

    val isReadyToForward: Boolean
        get() = settings.isMasterEnabled &&
            isDestinationValid &&
            enabledRuleCount > 0 &&
            permissionStatus.allRequiredSmsPermissionsGranted
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FamilySmsApplication
    private val repository = app.repository
    private val settingsRepository = app.settingsRepository

    private val permissionStatusFlow = MutableStateFlow(
        PermissionHelper.getPermissionStatus(application)
    )

    val uiState: StateFlow<MainUiState> = combine(
        repository.settingsFlow,
        repository.senderRulesFlow,
        repository.recentLogsFlow,
        repository.recentErrorsFlow,
        permissionStatusFlow
    ) { settings, rules, logs, errors, permissions ->
        MainUiState(
            settings = settings,
            senderRules = rules,
            recentLogs = logs,
            recentErrors = errors,
            permissionStatus = permissions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(permissionStatus = permissionStatusFlow.value)
    )

    fun refreshPermissions() {
        permissionStatusFlow.value = PermissionHelper.getPermissionStatus(getApplication())
    }

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMasterEnabled(enabled)
        }
    }

    fun saveDestinationNumber(number: String) {
        viewModelScope.launch {
            settingsRepository.setDestinationNumber(number)
        }
    }

    fun setIncludeTimestamp(include: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIncludeTimestamp(include)
        }
    }

    fun setDedupeWindowSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setDedupeWindowSeconds(seconds)
        }
    }

    fun setNotifyOnError(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyOnError(enabled)
        }
    }

    fun setNotifyOnForwarded(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyOnForwarded(enabled)
        }
    }

    fun addSenderRule(pattern: String, label: String, matchType: MatchType) {
        viewModelScope.launch {
            repository.addSenderRule(pattern, label, matchType)
        }
    }

    fun setSenderRuleEnabled(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setSenderRuleEnabled(ruleId, enabled)
        }
    }

    fun deleteSenderRule(rule: SenderRuleEntity) {
        viewModelScope.launch {
            repository.deleteSenderRule(rule)
        }
    }

    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            repository.deleteLog(logId)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
