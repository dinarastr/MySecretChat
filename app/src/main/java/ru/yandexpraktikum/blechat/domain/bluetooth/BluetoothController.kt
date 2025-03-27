package ru.yandexpraktikum.blechat.domain.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import ru.yandexpraktikum.blechat.domain.model.ConnectionState
import ru.yandexpraktikum.blechat.domain.model.ScannedBluetoothDevice

interface BluetoothController {
    val isBluetoothEnabled: StateFlow<Boolean>
    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<ScannedBluetoothDevice>>
    val connectedDevices: StateFlow<List<ScannedBluetoothDevice>>
    val errors: SharedFlow<String>
    
    fun startScan()
    fun stopScan()

    fun startAdvertising()
    fun stopAdvertising()
    
    fun startServer()
    fun stopServer()

    fun connectToDevice(device: ScannedBluetoothDevice): Flow<ConnectionState>
    
    suspend fun sendMessage(message: String): Boolean
    
    fun closeConnection()
    fun release()
}