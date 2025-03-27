package ru.yandexpraktikum.blechat.presentation.scanner

import ru.yandexpraktikum.blechat.domain.model.ScannedBluetoothDevice


data class ScannedDevicesState(
    val connectedDevices: List<ScannedBluetoothDevice> = emptyList(),
    val scannedDevices: List<ScannedBluetoothDevice> = emptyList(),
    val isBluetoothEnabled: Boolean = false,
    val errorMessage: String? = null,
    val isScanning: Boolean = false,
    val isAdvertising: Boolean = false
)