package ru.yandexpraktikum.blechat.presentation.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.yandexpraktikum.blechat.domain.bluetooth.BluetoothController
import ru.yandexpraktikum.blechat.domain.model.ConnectionState
import javax.inject.Inject

/**
 * TODO("Add documentation")
 */
@HiltViewModel
class ScannedDevicesViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
): ViewModel() {

    private val _state = MutableStateFlow(ScannedDevicesState())

    val state = combine(
        bluetoothController.connectedDevices,
        bluetoothController.scannedDevices,
        bluetoothController.isBluetoothEnabled,
        _state
    ) { connectedDevices, scannedDevices, isEnabled, state ->
        state.copy(
            connectedDevices = connectedDevices,
            scannedDevices = scannedDevices,
            isBluetoothEnabled = isEnabled,
            isScanning = state.isScanning,
            isAdvertising = state.isAdvertising
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ScannedDevicesState()
    )

    fun onEvent(event: ScannedDevicesEvent) {
        when (event) {
            is ScannedDevicesEvent.ToggleAdvertising -> {
                if (!state.value.isBluetoothEnabled) {
                    _state.update { it.copy(errorMessage = "Bluetooth is not enabled") }
                    return
                }
                _state.update { it.copy(isAdvertising = !it.isAdvertising) }
                if (_state.value.isAdvertising) {
                    startAdvertising()
                } else {
                    bluetoothController.stopServer()
                }
            }
            is ScannedDevicesEvent.ToggleScan -> {
                if (!state.value.isBluetoothEnabled) {
                    _state.update { it.copy(errorMessage = "Bluetooth is not enabled") }
                    return
                }
                _state.update { it.copy(isScanning = !it.isScanning) }
                if (_state.value.isScanning) {
                    startScan()
                } else {
                    bluetoothController.stopScan()
                }
            }

            is ScannedDevicesEvent.ConnectToDevice -> {
                viewModelScope.launch {
                    bluetoothController.connectToDevice(event.device)
                        .collect { connectionState ->
                            when (connectionState) {
                                is ConnectionState.Connected -> {
                                    Log.i("ScannedDevicesViewModel", "Connected")
                                }
                                is ConnectionState.Disconnected -> {
                                    Log.i("ScannedDevicesViewModel", "Disconnected")
                                    _state.update {
                                        it.copy(errorMessage = "Device disconnected")
                                    }
                                }
                                else -> Unit
                            }
                        }
                }
            }
        }
    }

    private fun startScan() {
        viewModelScope.launch {
            try {
                bluetoothController.startScan()
                delay(SCAN_PERIOD)
                if (_state.value.isScanning) {
                    _state.update { it.copy(isScanning = false) }
                    bluetoothController.stopScan()
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = "Failed to scan: ${e.localizedMessage}"
                    )
                }
                bluetoothController.stopScan()
            }
        }
    }


    private fun startAdvertising() {
        viewModelScope.launch {
            try {
                bluetoothController.startServer()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isAdvertising = false,
                        errorMessage = "Failed to advertise: ${e.localizedMessage}"
                    )
                }
                bluetoothController.stopServer()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.stopScan()
        bluetoothController.stopAdvertising()
        bluetoothController.release()
    }

    companion object {
        private const val SCAN_PERIOD = 20000L // 10 seconds
    }
}