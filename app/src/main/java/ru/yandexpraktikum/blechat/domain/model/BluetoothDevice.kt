package ru.yandexpraktikum.blechat.domain.model

data class BluetoothDevice(
    val name: String?,
    val address: String,
    val isConnected: Boolean = false
)