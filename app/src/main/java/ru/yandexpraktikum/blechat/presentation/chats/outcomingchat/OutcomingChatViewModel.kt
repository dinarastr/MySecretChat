package ru.yandexpraktikum.blechat.presentation.chats.outcomingchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.yandexpraktikum.blechat.domain.bluetooth.BLEClientController
import ru.yandexpraktikum.blechat.domain.model.Message
import ru.yandexpraktikum.blechat.presentation.chats.ChatEvent
import ru.yandexpraktikum.blechat.presentation.chats.ChatState
import javax.inject.Inject

@HiltViewModel
class OutcomingChatViewModel @Inject constructor(
    private val clientController: BLEClientController
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ChatState()
    )

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.LoadMessages -> {
                viewModelScope.launch {
                    clientController.scannedDevices.collect { devices ->
                        val device = devices.find { it.address == event.address }
                        _state.update { it.copy(
                            connectedDevice = device,
                            isConnected = device != null,
                            messages = device?.messages ?: emptyList())
                        }
                    }
                }
            }
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
                    clientController.sendMessage(
                        event.message,
                        event.address
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clientController.closeConnection()
    }
}