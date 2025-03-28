package ru.yandexpraktikum.blechat.domain.bluetooth

import kotlinx.coroutines.flow.StateFlow
import ru.yandexpraktikum.blechat.domain.model.ScannedBluetoothDevice

interface BLEClientController {
    val isBluetoothEnabled: StateFlow<Boolean>
    val isLocationEnabled: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<ScannedBluetoothDevice>>

    fun updateLocationState()

    fun startScan()
    fun stopScan()

    fun connectToDevice(device: ScannedBluetoothDevice): Boolean

    suspend fun sendMessage(message: String, deviceAddress: String): Boolean

    fun closeConnection()
    fun release()
}