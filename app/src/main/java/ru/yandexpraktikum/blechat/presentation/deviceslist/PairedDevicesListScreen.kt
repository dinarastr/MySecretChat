package ru.yandexpraktikum.blechat.presentation.deviceslist

import android.bluetooth.BluetoothAdapter
import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairedDevicesListScreen(
    onAddNewDeviceClick: () -> Unit,
    onDeviceClick: (String) -> Unit,
    viewModel: PairedDevicesListViewModel = hiltViewModel()
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
                title = { Text("Paired Devices") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
               onAddNewDeviceClick()
            }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add device"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(state.pairedDevices) { device ->
                    DeviceItem(
                        device = device,
                        onClick = { onDeviceClick(device.address) }
                    )
                }
            }
        }
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
        }
    }
}