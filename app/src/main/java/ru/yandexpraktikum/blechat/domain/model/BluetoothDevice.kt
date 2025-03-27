package ru.yandexpraktikum.blechat.domain.model

data class ScannedBluetoothDevice(
    val name: String?,
    val address: String,
    val isConnected: Boolean = false,
    val messages: List<Message> = emptyList()
)