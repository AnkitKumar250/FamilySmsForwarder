package com.familysms.forwarder.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val receiveSmsGranted: Boolean,
    val sendSmsGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val batteryOptimizationIgnored: Boolean
) {
    val allRequiredSmsPermissionsGranted: Boolean
        get() = receiveSmsGranted && sendSmsGranted
}

object PermissionHelper {

    fun requiredRuntimePermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }

    fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun getPermissionStatus(context: Context): PermissionStatus {
        val receiveSms = isGranted(context, Manifest.permission.RECEIVE_SMS)
        val sendSms = isGranted(context, Manifest.permission.SEND_SMS)
        val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        val batteryIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)

        return PermissionStatus(
            receiveSmsGranted = receiveSms,
            sendSmsGranted = sendSms,
            postNotificationsGranted = notifications,
            batteryOptimizationIgnored = batteryIgnored
        )
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
