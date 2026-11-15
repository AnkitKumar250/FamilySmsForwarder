package com.familysms.forwarder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.familysms.forwarder.domain.MatchType
import com.familysms.forwarder.domain.PhoneNumberNormalizer

@Composable
fun AddSenderDialog(
    onDismiss: () -> Unit,
    onConfirm: (pattern: String, label: String, matchType: MatchType) -> Unit
) {
    var senderPattern by remember { mutableStateOf("") }
    var displayLabel by remember { mutableStateOf("") }
    var selectedMatchType by remember { mutableStateOf(MatchType.EXACT) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add Monitored Sender")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = senderPattern,
                    onValueChange = { input ->
                        senderPattern = input
                        showError = false
                        // Auto-suggest Contains when user types an alphanumeric short sender ID without prefix
                        if (!PhoneNumberNormalizer.isPhoneNumberLike(input) && input.length >= 3 && !input.contains("-")) {
                            selectedMatchType = MatchType.CONTAINS
                        }
                    },
                    label = { Text("Sender ID or Phone Number") },
                    placeholder = { Text("e.g. +15551234567 or HDFCBK") },
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Please enter a valid sender ID or phone number.") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = displayLabel,
                    onValueChange = { displayLabel = it },
                    label = { Text("Friendly Label (Optional)") },
                    placeholder = { Text("e.g. Family Bank Alerts") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Match Mode",
                    style = MaterialTheme.typography.labelLarge
                )

                Column(modifier = Modifier.selectableGroup()) {
                    MatchType.entries.forEach { matchType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedMatchType == matchType),
                                    onClick = { selectedMatchType = matchType },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(
                                selected = (selectedMatchType == matchType),
                                onClick = null
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = matchType.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = matchType.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = senderPattern.trim()
                    if (trimmed.isEmpty()) {
                        showError = true
                    } else {
                        onConfirm(trimmed, displayLabel.trim(), selectedMatchType)
                    }
                }
            ) {
                Text("Add Sender")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
