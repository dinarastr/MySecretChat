package ru.yandexpraktikum.blechat.presentation.deviceslist

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

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceListState())
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        bluetoothController.isBluetoothEnabled,
        _state
    ) { scannedDevices, pairedDevices, isEnabled, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            isBluetoothEnabled = isEnabled,
            isScanning = state.isScanning,
            isAdvertising = state.isAdvertising
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DeviceListState()
    )

    fun onEvent(event: DeviceListEvent) {
        when (event) {
            is DeviceListEvent.ToggleAdvertising -> {
                if (!state.value.isBluetoothEnabled) {
                    _state.update { it.copy(errorMessage = "Bluetooth is not enabled") }
                    return
                }
                _state.update { it.copy(isAdvertising = !it.isAdvertising) }
                if (_state.value.isAdvertising) {
                    startAdvertising()
                } else {
                    bluetoothController.stopAdvertising()
                }
            }
            is DeviceListEvent.ToggleScan -> {
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
            is DeviceListEvent.ConnectToDevice -> {
                viewModelScope.launch {
                    bluetoothController.connectToDevice(event.device)
                        .collect { connectionState ->
                            when (connectionState) {
                                is ConnectionState.Connected -> {
                                    // Handle successful connection
                                }
                                is ConnectionState.Disconnected -> {
                                    _state.update { 
                                        it.copy(errorMessage = "Device disconnected")
                                    }
                                }
                                else -> Unit
                            }
                        }
                }
            }
            is DeviceListEvent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
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
                bluetoothController.startAdvertising()
                delay(SCAN_PERIOD)
                if (_state.value.isAdvertising) {
                    _state.update { it.copy(isAdvertising = false) }
                    bluetoothController.stopAdvertising()
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isAdvertising = false,
                        errorMessage = "Failed to advertise: ${e.localizedMessage}"
                    )
                }
                bluetoothController.stopAdvertising()
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