package ru.yandexpraktikum.blechat.presentation.scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice

/**
 * TODO("Add documentation")
 */

val ALL_BLE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
    )
} else {
    arrayOf(
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedDevicesListScreen(
    onNavigateUp: () -> Unit,
    onDeviceClick: (String) -> Unit,
    viewModel: ScannedDevicesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {

        } else {
            Toast.makeText(context, "Failed to enable Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    val advertiseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            viewModel.onEvent(ScannedDevicesEvent.ToggleAdvertising)
        } else {
            Toast.makeText(context, "Failed to enable advertising", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state.isBluetoothEnabled) {
        if (!state.isBluetoothEnabled) {
            enableBluetoothLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanned Devices") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateUp
                    ){
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },

    ) { paddingValues ->

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(state.scannedDevices) { device ->
                    DeviceItem(
                        device = device,
                        onClick = {
                            if (device.isConnected) {
                                onDeviceClick(device.address)
                            } else {
                                viewModel.onEvent(ScannedDevicesEvent.ConnectToDevice(device))
                            }
                        }
                    )
                }
            }

            if (state.isScanning) {
                Text(
                    text = "Scanning for devices...",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (state.isAdvertising) {
                Text(
                    text = "Advertising for devices...",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            GrantPermissionsButton(onPermissionGranted = {
                viewModel.onEvent(ScannedDevicesEvent.ToggleScan)
            }, state = state)
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                    advertiseLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
                } else {
                    viewModel.onEvent(ScannedDevicesEvent.ToggleAdvertising)
                }
            }) {
                Text(if (state.isScanning) "Stop Server" else "Start server")
            }

        }
    }
}

@Composable
fun GrantPermissionsButton(onPermissionGranted: () -> Unit, state: ScannedDevicesState) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            // User has granted all permissions
            onPermissionGranted()
        }
        else {
            // TODO: handle potential rejection in the usual way
        }
    }

    // User presses this button to request permissions
    Button(onClick = { launcher.launch(ALL_BLE_PERMISSIONS) }) {
        Text(if (state.isScanning) "Stop Scan" else "Scan for Devices")
    }
}

@Composable
private fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = device.name ?: "Unknown Device",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.bodyMedium
            )
            if (device.isConnected) {
                Text("Connected", color = Color.Green)
            }
        }
    }
}