package ru.yandexpraktikum.blechat.presentation.deviceslist

import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice

data class DeviceListState(
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val isScanning: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val errorMessage: String? = null,
    val isAdvertising: Boolean = false,
)