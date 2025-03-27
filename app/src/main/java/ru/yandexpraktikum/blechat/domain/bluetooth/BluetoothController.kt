package ru.yandexpraktikum.blechat.domain.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice
import ru.yandexpraktikum.blechat.domain.model.ConnectionState

interface BluetoothController {
    val isBluetoothEnabled: StateFlow<Boolean>
    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val pairedDevices: StateFlow<List<BluetoothDevice>>
    val errors: SharedFlow<String>
    
    fun startScan()
    fun stopScan()

    fun startAdvertising()
    fun stopAdvertising()
    
    fun startServer()
    fun stopServer()

    fun connectToDevice(device: BluetoothDevice): Flow<ConnectionState>
    
    suspend fun sendMessage(message: String): Boolean
    
    fun closeConnection()
    fun release()
}