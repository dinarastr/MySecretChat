package ru.yandexpraktikum.blechat.presentation.scanner

import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice

/**
 * TODO("Add documentation")
 */
sealed class ScannedDevicesEvent {
    data object ToggleScan : ScannedDevicesEvent()
    data class ConnectToDevice(val device: BluetoothDevice) : ScannedDevicesEvent()
    data object ToggleAdvertising : ScannedDevicesEvent()
}