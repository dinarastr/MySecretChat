package ru.yandexpraktikum.blechat.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.yandexpraktikum.blechat.domain.bluetooth.BLEClientController
import ru.yandexpraktikum.blechat.domain.model.Message
import ru.yandexpraktikum.blechat.domain.model.ScannedBluetoothDevice
import ru.yandexpraktikum.blechat.utils.checkForConnectPermission
import java.nio.charset.Charset
import java.util.UUID
import javax.inject.Inject

class BLEClientControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val locationManager: LocationManager,
    private val viewModelScope: CoroutineScope
): BLEClientController {

    private val bleScanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    private var currentGatt: BluetoothGatt? = null

    private val serviceUUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
    private val writeCharUUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b")
    private val notifyCharUUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4c")

    private val _isBluetoothEnabled = MutableStateFlow(false)
    override val isBluetoothEnabled: StateFlow<Boolean>
        get() = _isBluetoothEnabled.asStateFlow()

    private val _isLocationEnabled = MutableStateFlow(false)
    override val isLocationEnabled: StateFlow<Boolean>
        get() = _isLocationEnabled.asStateFlow()

    init {
        initializeBluetoothState()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            updateLocationState()
        }
    }

    private fun initializeBluetoothState() {
        try {
            _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        } catch (e: Exception) {
            Log.e("BLE", "Failed to initialize Bluetooth state", e)
        }
    }

    override fun updateLocationState() {
        try {
            _isLocationEnabled.value = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.e("BLE", "Failed to initialize Location state", e)
        }
    }

    private val _scannedDevices = MutableStateFlow<List<ScannedBluetoothDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<ScannedBluetoothDevice>>
        get() = _scannedDevices.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            context.checkForConnectPermission {
                val bluetoothDevice = ScannedBluetoothDevice(
                    name = device.name,
                    address = device.address
                )
                _scannedDevices.update { devices ->
                    if (devices.none { it.address == bluetoothDevice.address }) {
                        devices + bluetoothDevice
                    } else devices
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BLE", "Scan failed with error code: $errorCode")
        }
    }

    override fun startScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        bleScanner?.startScan(scanCallback)
    }

    override fun stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        bleScanner?.stopScan(scanCallback)
        _scannedDevices.update {
            it.filter { device ->
                device.isConnected
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        context.checkForConnectPermission {
                            gatt.discoverServices()
                        }
                        _scannedDevices.update { devices ->
                            devices.map {
                                if (it.address == gatt.device.address) {
                                    it.copy(isConnected = true)
                                } else it
                            }
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _scannedDevices.update { devices ->
                            devices.map {
                                if (it.address == gatt.device.address) {
                                    it.copy(isConnected = false)
                                } else it
                            }
                        }
                        closeConnection()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(serviceUUID)
                val notifyCharacteristic = service?.getCharacteristic(notifyCharUUID)
                if (notifyCharacteristic != null) {
                    context.checkForConnectPermission {
                        gatt.setCharacteristicNotification(notifyCharacteristic, true)
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic.uuid == notifyCharUUID) {
                val message = String(characteristic.value, Charset.defaultCharset())
                viewModelScope.launch {
                    _scannedDevices.update { devices ->
                        devices.map {
                            if (it.address == gatt.device.address) {
                                it.copy(
                                    messages = it.messages + Message(
                                        text = message,
                                        senderAddress = gatt.device.address,
                                        isFromLocalUser = false
                                    )
                                )
                            } else it
                        }
                    }
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == notifyCharUUID) {
                val message = String(value, Charset.defaultCharset())
                viewModelScope.launch {
                    _scannedDevices.update { devices ->
                        devices.map {
                            if (it.address == gatt.device.address) {
                                it.copy(
                                    messages = it.messages + Message(
                                        text = message,
                                        senderAddress = gatt.device.address,
                                        isFromLocalUser = false
                                    )
                                )
                            } else it
                        }
                    }
                }
            }
        }
    }

    override fun connectToDevice(device: ScannedBluetoothDevice): Boolean {

        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        context.checkForConnectPermission {
            currentGatt = bluetoothDevice?.connectGatt(context, false, gattCallback)
        }
        return currentGatt != null
    }

    override suspend fun sendMessage(message: String, deviceAddress: String): Boolean {
        val gattService = currentGatt?.getService(serviceUUID)
        val characteristic = gattService?.getCharacteristic(writeCharUUID)

        return if (characteristic != null) {
            characteristic.setValue(message.toByteArray(Charset.defaultCharset()))
            context.checkForConnectPermission {
                currentGatt?.writeCharacteristic(characteristic)
            }
            _scannedDevices.update { devices ->
                devices.map {
                    if (it.address == deviceAddress) {
                        it.copy(
                            messages = it.messages + Message(
                                text = message,
                                senderAddress = bluetoothAdapter?.address ?: "",
                                isFromLocalUser = true
                            )
                        )
                    } else it
                }
            }
            true
        } else false
    }

    override fun closeConnection() {
        context.checkForConnectPermission {
            currentGatt?.close()
        }
        currentGatt = null
    }

    override fun release() {
        closeConnection()
    }
}