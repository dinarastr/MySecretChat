package ru.yandexpraktikum.blechat.presentation.scanner

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
import ru.yandexpraktikum.blechat.domain.bluetooth.BLEClientController
import ru.yandexpraktikum.blechat.domain.bluetooth.BLEServerController
import javax.inject.Inject

/**
 * TODO("Add documentation")
 */
@HiltViewModel
class ScannedDevicesViewModel @Inject constructor(
    private val serverController: BLEServerController,
    private val clientController: BLEClientController
): ViewModel() {

    private val _state = MutableStateFlow(ScannedDevicesState())

    val state = combine(
        serverController.connectedDevices,
        clientController.scannedDevices,
        clientController.isBluetoothEnabled,
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
                    serverController.stopServer()
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
                    clientController.stopScan()
                }
            }

            is ScannedDevicesEvent.ConnectToDevice -> {
                clientController.connectToDevice(event.device)
            }
        }
    }

    private fun startScan() {
        viewModelScope.launch {
            try {
                clientController.startScan()
                delay(SCAN_PERIOD)
                if (_state.value.isScanning) {
                    _state.update { it.copy(isScanning = false) }
                    clientController.stopScan()
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = "Failed to scan: ${e.localizedMessage}"
                    )
                }
                clientController.stopScan()
            }
        }
    }


    private fun startAdvertising() {
        viewModelScope.launch {
            try {
                serverController.startServer()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isAdvertising = false,
                        errorMessage = "Failed to advertise: ${e.localizedMessage}"
                    )
                }
                serverController.stopServer()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clientController.stopScan()
        serverController.stopAdvertising()
        clientController.release()
        serverController.release()
    }

    companion object {
        private const val SCAN_PERIOD = 20000L // 10 seconds
    }
}