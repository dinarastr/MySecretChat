package ru.yandexpraktikum.blechat.presentation.scanner

import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice

/**
 * TODO("Add documentation")
 */
data class ScannedDevicesState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val isBluetoothEnabled: Boolean = false,
    val errorMessage: String? = null,
    val isScanning: Boolean = false,
    val isAdvertising: Boolean = false
)