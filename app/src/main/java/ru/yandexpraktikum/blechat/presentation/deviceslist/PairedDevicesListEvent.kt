package ru.yandexpraktikum.blechat.presentation.deviceslist

import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice

sealed class PairedDevicesListEvent {
    data class ConnectToDevice(val device: BluetoothDevice) : PairedDevicesListEvent()
}