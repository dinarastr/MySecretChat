package ru.yandexpraktikum.blechat.presentation.chats

import ru.yandexpraktikum.blechat.domain.model.Message
import ru.yandexpraktikum.blechat.domain.model.ScannedBluetoothDevice

data class ChatState(
    val connectedDevice: ScannedBluetoothDevice? = null,
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<Message> = emptyList(),
    )