package ru.yandexpraktikum.blechat.presentation.chats.incomingchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.yandexpraktikum.blechat.domain.bluetooth.BLEServerController
import ru.yandexpraktikum.blechat.presentation.chats.ChatEvent
import ru.yandexpraktikum.blechat.presentation.chats.ChatState
import javax.inject.Inject

@HiltViewModel
class IncomingChatViewModel @Inject constructor(
    private val serverController: BLEServerController
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ChatState()
    )

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SendMessage -> {
                viewModelScope.launch {
                    serverController.sendServerMessage(
                        event.message,
                        event.address
                    )
                }
            }

            is ChatEvent.LoadMessages -> {
                viewModelScope.launch {
                    serverController.connectedDevices.collect { devices ->
                        val device = devices.find { it.address == event.address }
                        _state.update {
                            it.copy(
                                connectedDevice = device,
                                isConnected = device != null,
                                messages = device?.messages ?: emptyList()
                            )
                        }
                    }
                }
            }
        }
    }
}