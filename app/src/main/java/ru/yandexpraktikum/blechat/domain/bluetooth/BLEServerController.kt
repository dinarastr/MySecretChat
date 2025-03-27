package ru.yandexpraktikum.blechat.domain.bluetooth

import kotlinx.coroutines.flow.StateFlow
import ru.yandexpraktikum.blechat.domain.model.ScannedBluetoothDevice

interface BLEServerController {
    val isBluetoothEnabled: StateFlow<Boolean>
    val connectedDevices: StateFlow<List<ScannedBluetoothDevice>>

    fun startAdvertising()
    fun stopAdvertising()

    fun startServer()
    fun stopServer()

    suspend fun sendServerMessage(message: String, deviceAddress: String): Boolean

    fun closeConnection()
    fun release()
}