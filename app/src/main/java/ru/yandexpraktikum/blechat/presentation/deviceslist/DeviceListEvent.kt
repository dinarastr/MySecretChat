package ru.yandexpraktikum.blechat.presentation.deviceslist

import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice

sealed class DeviceListEvent {
    data object ToggleScan : DeviceListEvent()
    data class ConnectToDevice(val device: BluetoothDevice) : DeviceListEvent()
    data object DismissError : DeviceListEvent()
    data object ToggleAdvertising : DeviceListEvent()
}