package ru.yandexpraktikum.blechat.presentation.scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.time.delay
import ru.yandexpraktikum.blechat.R
import ru.yandexpraktikum.blechat.domain.model.ScannedBluetoothDevice
import ru.yandexpraktikum.blechat.utils.ALL_BLE_PERMISSIONS
import ru.yandexpraktikum.blechat.utils.advertiseLauncher
import ru.yandexpraktikum.blechat.utils.bluetoothLauncher
import ru.yandexpraktikum.blechat.utils.connectLauncher
import ru.yandexpraktikum.blechat.utils.locationLauncher
import ru.yandexpraktikum.blechat.utils.scanPermissionsLauncher


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedDevicesListScreen(
    onDeviceClick: (String, Boolean) -> Unit,
    viewModel: ScannedDevicesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val bluetoothLauncher = context.bluetoothLauncher(
        updateBluetoothEnabled = {
            viewModel.onEvent(ScannedDevicesEvent.CheckBluetoothSettings)
        }
    )

    val locationLauncher = locationLauncher(
        state.isLocationEnabled,
        onLocationEnabled = {
            viewModel.onEvent(ScannedDevicesEvent.ToggleScan)
        }
    ) {
        viewModel.onEvent(ScannedDevicesEvent.CheckLocationSettings)
    }

    val advertiseLauncher = advertiseLauncher {
        viewModel.onEvent(ScannedDevicesEvent.ToggleAdvertising)
    }

    val connectLauncher = context.connectLauncher(bluetoothLauncher)

    LaunchedEffect(state.isBluetoothEnabled) {
        if (!state.isBluetoothEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            connectLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else if (!state.isBluetoothEnabled) {
            bluetoothLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scanned_devices)) }
            )
        }) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                if (state.connectedDevices.isNotEmpty()) {
                    items(state.connectedDevices) { device ->
                        DeviceItem(
                            device = device,
                            onClick = {
                                onDeviceClick(device.address, true)
                            })
                    }
                } else {
                    items(state.scannedDevices) { device ->
                        DeviceItem(
                            device = device,
                            onClick = {
                                if (device.isConnected) {
                                    onDeviceClick(device.address, false)
                                } else {
                                    viewModel.onEvent(ScannedDevicesEvent.ConnectToDevice(device))
                                }
                            }
                        )
                    }
                }
            }

            if (state.isScanning) {
                Text(
                    text = stringResource(R.string.scanning_for_devices),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (state.isAdvertising) {
                Text(
                    text = stringResource(R.string.advertising_for_devices),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            ScanningComponent(
                checkLocation = {
                    viewModel.onEvent(ScannedDevicesEvent.CheckLocationSettings)
                },
                context = context,
                onAllNecessaryPermissionGranted = {
                    viewModel.onEvent(ScannedDevicesEvent.ToggleScan)
                },
                state = state
            )
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    advertiseLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
                } else {
                    viewModel.onEvent(ScannedDevicesEvent.ToggleAdvertising)
                }
            }) {
                Text(
                    if (state.isAdvertising) stringResource(R.string.stop_server) else stringResource(
                        R.string.start_server
                    )
                )
            }
        }
    }
}

@Composable
fun ScanningComponent(
    checkLocation: () -> Unit,
    context: Context,
    onAllNecessaryPermissionGranted: () -> Unit,
    state: ScannedDevicesState
) {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    val permissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) arrayOf(
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) else arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

    val requestPermissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsList ->
            if (permissionsList.values.all { it }) {
                onAllNecessaryPermissionGranted()
            } else {
                Log.e("scanner", "necessary permissions rejected: ${permissionsList.filter { !it.value }}")
            }
        }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        checkLocation()
        if (state.isLocationEnabled) {
            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()
            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionsLauncher.launch(permissionsToRequest)
            } else {
                onAllNecessaryPermissionGranted()
            }
        }
    }

    Button(
        onClick = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && state.isLocationEnabled.not()) {
                locationSettingsLauncher.launch(intent)
            } else {
                val permissionsToRequest = permissions.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }.toTypedArray()
                if (permissionsToRequest.isNotEmpty()) {
                    requestPermissionsLauncher.launch(permissionsToRequest)
                } else {
                    onAllNecessaryPermissionGranted()
                }
            }
        }
    ) {
        Text(if (state.isScanning) stringResource(R.string.stop_scan) else stringResource(R.string.scan_for_devices))
    }

}

@Composable
fun ScanButton(context: Context, onPermissionGranted: () -> Unit, state: ScannedDevicesState) {
    val launcher = scanPermissionsLauncher {
        onPermissionGranted()
    }

    Button(onClick = {
        if (ALL_BLE_PERMISSIONS.any {
                ActivityCompat.checkSelfPermission(
                    context,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }) {
            launcher.launch(ALL_BLE_PERMISSIONS)
        } else {
            onPermissionGranted()
        }

    }) {
        Text(if (state.isScanning) stringResource(R.string.stop_scan) else stringResource(R.string.scan_for_devices))
    }
}

@Composable
private fun DeviceItem(
    device: ScannedBluetoothDevice,
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
                text = device.name ?: stringResource(R.string.unknown_device),
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