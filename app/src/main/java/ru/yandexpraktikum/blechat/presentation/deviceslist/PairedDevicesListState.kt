package ru.yandexpraktikum.blechat.presentation.deviceslist

import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice

data class PairedDevicesListState(
    val isBluetoothEnabled: Boolean = false,
    val errorMessage: String? = null,
    val pairedDevices: List<BluetoothDevice> = emptyList()
)