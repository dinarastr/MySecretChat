package ru.yandexpraktikum.blechat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.yandexpraktikum.blechat.domain.bluetooth.BluetoothController
import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice
import ru.yandexpraktikum.blechat.domain.model.Message
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ChatState()
    )

    fun initializeChat(deviceAddress: String) {
        viewModelScope.launch {
            val device = BluetoothDevice(
                name = deviceAddress, // You might want to get the actual name from paired devices
                address = deviceAddress
            )
            _state.update { it.copy(connectedDevice = device) }
            
            bluetoothController.connectToDevice(device)
                .collect { connectionState ->
                    // Handle connection state changes
                }
        }
    }

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SendMessage -> {
                viewModelScope.launch {
                    val message = Message(
                        text = event.message,
                        senderAddress = event.address,
                        isFromLocalUser = true
                    )
                    _state.update { it.copy(
                        messages = it.messages + message
                    )}
                    bluetoothController.sendMessage(event.message)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.closeConnection()
    }
}