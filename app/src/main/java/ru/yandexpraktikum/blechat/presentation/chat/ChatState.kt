package ru.yandexpraktikum.blechat.presentation.chat

import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice
import ru.yandexpraktikum.blechat.domain.model.Message

data class ChatState(
    val messages: List<Message> = emptyList(),
    val connectedDevice: BluetoothDevice? = null,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null
)