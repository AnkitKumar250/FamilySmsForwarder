package com.familysms.forwarder.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familysms.forwarder.domain.ForwardStatus
import com.familysms.forwarder.ui.theme.ErrorRed
import com.familysms.forwarder.ui.theme.ErrorRedContainer
import com.familysms.forwarder.ui.theme.SuccessGreen
import com.familysms.forwarder.ui.theme.SuccessGreenContainer
import com.familysms.forwarder.ui.theme.WarningAmber
import com.familysms.forwarder.ui.theme.WarningAmberContainer

@Composable
fun StatusBadge(
    status: ForwardStatus,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = when (status) {
        ForwardStatus.DELIVERED,
        ForwardStatus.SENT -> SuccessGreenContainer to SuccessGreen
        ForwardStatus.QUEUED,
        ForwardStatus.SENDING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ForwardStatus.SKIPPED_DUPLICATE -> WarningAmberContainer to WarningAmber
        ForwardStatus.FAILED -> ErrorRedContainer to ErrorRed
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status.label,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
