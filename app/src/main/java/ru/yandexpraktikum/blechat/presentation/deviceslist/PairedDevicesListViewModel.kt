package ru.yandexpraktikum.blechat.presentation.deviceslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
class PairedDevicesListViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val _state = MutableStateFlow(PairedDevicesListState())
    val state = combine(
        bluetoothController.pairedDevices,
        bluetoothController.isBluetoothEnabled,
        _state
    ) { pairedDevices, isEnabled, state ->
        state.copy(
            pairedDevices = pairedDevices,
            isBluetoothEnabled = isEnabled,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PairedDevicesListState()
    )

    fun onEvent(event: PairedDevicesListEvent) {
        when (event) {
            is PairedDevicesListEvent.ConnectToDevice -> {
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
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}