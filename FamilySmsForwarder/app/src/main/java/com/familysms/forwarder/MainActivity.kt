package com.familysms.forwarder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.familysms.forwarder.ui.FamilySmsApp
import com.familysms.forwarder.ui.MainViewModel
import com.familysms.forwarder.ui.theme.FamilySmsForwarderTheme
import com.familysms.forwarder.util.BatteryOptimizationHelper
import com.familysms.forwarder.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FamilySmsForwarderTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.refreshPermissions()
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshPermissions()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                FamilySmsApp(
                    viewModel = viewModel,
                    onRequestPermissions = {
                        permissionLauncher.launch(PermissionHelper.requiredRuntimePermissions())
                    },
                    onOpenAppSettings = {
                        PermissionHelper.openAppDetailsSettings(this@MainActivity)
                    },
                    onOpenBatterySettings = {
                        BatteryOptimizationHelper.openBatteryOptimizationSettings(this@MainActivity)
                    }
                )
            }
        }
    }
}
