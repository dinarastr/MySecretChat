package ru.yandexpraktikum.blechat.presentation.scanner

import ru.yandexpraktikum.blechat.domain.model.ScannedBluetoothDevice


/**
 * TODO("Add documentation")
 */
sealed class ScannedDevicesEvent {
    data object ToggleScan : ScannedDevicesEvent()
    data class ConnectToDevice(val device: ScannedBluetoothDevice) : ScannedDevicesEvent()
    data object ToggleAdvertising : ScannedDevicesEvent()
    data object CheckLocationSettings: ScannedDevicesEvent()
}