package ru.yandexpraktikum.blechat.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat

val ALL_BLE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
    )
} else
    arrayOf(
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

@Composable
fun locationLauncher(
    isLocationEnabled: Boolean,
    onLocationEnabled: () -> Unit,
    checkLocationSettings: () -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        checkLocationSettings()
        if (isLocationEnabled) {
            onLocationEnabled()
        } else {
            Log.e("scanner", "Failed to enable location")
        }

    }
    return locationSettingsLauncher
}

fun Context.checkForConnectPermission(action: () -> Unit) {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED -> action.invoke()
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> action.invoke()
    }
}

@Composable
fun Context.bluetoothLauncher(): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Log.i("scanner", "Bluetooth enabled")
        } else {
            Toast.makeText(this, "Failed to enable Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }
    return enableBluetoothLauncher
}


@Composable
fun advertiseLauncher(
    onAdvertisingEnabled: () -> Unit,
): ManagedActivityResultLauncher<String, Boolean> {
    val advertiseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onAdvertisingEnabled()
        } else {
            Log.i("Server", "Failed to enable advertising")
        }
    }
    return advertiseLauncher
}

@Composable
fun Context.connectLauncher(
    bluetoothLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
): ManagedActivityResultLauncher<String, Boolean> {
    val connectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            bluetoothLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            )
        } else {
            Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show()
        }
    }
    return connectLauncher
}

@Composable
fun scanPermissionsLauncher(
    onPermissionsGranted: () -> Unit
): ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            onPermissionsGranted()
        } else {
            Log.e("scanner", "necessary permissions rejected")
        }
    }
    return launcher
}


