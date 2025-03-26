package ru.yandexpraktikum.blechat.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.yandexpraktikum.blechat.domain.bluetooth.BluetoothController
import ru.yandexpraktikum.blechat.domain.model.BluetoothDevice
import ru.yandexpraktikum.blechat.domain.model.ConnectionState
import java.nio.charset.Charset
import java.util.UUID
import javax.inject.Inject

/**
 * TODO("Add documentation")
 */
class BluetoothControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val viewModelScope: CoroutineScope
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BLUETOOTH_SERVICE)
                as? BluetoothManager
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }


    private val bleScanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    private val bluetoothLeAdvertiser by lazy {
        bluetoothAdapter?.bluetoothLeAdvertiser
    }

    private val serviceUuid = UUID.fromString("79141d83-a45a-4063-8cb5-8a34ac38e3c7")

    private val _isBluetoothEnabled = MutableStateFlow(false)
    override val isBluetoothEnabled: StateFlow<Boolean>
        get() = _isBluetoothEnabled.asStateFlow()

    private val _connectionState = MutableSharedFlow<ConnectionState>()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDevice>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDevice>>
        get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    init {
        initializeBluetoothState()
    }

    private fun initializeBluetoothState() {
        try {
            _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
            if (_isBluetoothEnabled.value) {
                updatePairedDevices()
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _errors.emit("Failed to initialize Bluetooth: ${e.localizedMessage}")
            }
        }
    }

    private fun updatePairedDevices() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothAdapter?.bondedDevices?.map { device ->
            Log.i("devices", device.toString())
            BluetoothDevice(
                name = device.name,
                address = device.address
            )
        }?.also { devices ->
            _pairedDevices.update { devices }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val bluetoothDevice = BluetoothDevice(
                name = device.name,
                address = device.address
            )
            _scannedDevices.update { devices ->
                if (devices.none { it.address == bluetoothDevice.address }) {
                    devices + bluetoothDevice
                } else devices
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            viewModelScope.launch {
                _errors.emit("Scan failed with error code: $errorCode")
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d("BLE", "Advertising started successfully")
            viewModelScope.launch {
                _connectionState.emit(ConnectionState.Advertising)
            }
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE", "Advertising failed with error: $errorCode")
            viewModelScope.launch {
                _errors.emit("Failed to start advertising: $errorCode")
            }
        }
    }

    override fun startAdvertising() {

        if (!bluetoothAdapter?.isEnabled!!) {
            Log.e("BLE", "Bluetooth is not enabled")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "Bluetooth advertise permission not granted")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(serviceUUID)) // Note: Use serviceUUID instead of serviceUuid
            .build()

        try {
            Log.d("BluetoothAdvertiser", "startAdvertising() called")
            Log.d("BluetoothAdvertiser", "advertiseSettings: $settings")
            Log.d("BluetoothAdvertiser", "advertiseData: $data")
            Log.d("BluetoothAdvertiser", "advertiseCallback: $advertiseCallback")
            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            Log.e("BLE", "Failed to start advertising", e)
            viewModelScope.launch {
                _errors.emit("Failed to start advertising: ${e.localizedMessage}")
            }
        }
    }

    override fun stopAdvertising() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            Log.d("BLE", "Advertising stopped")
        } catch (e: Exception) {
            Log.e("BLE", "Failed to stop advertising", e.fillInStackTrace())
            viewModelScope.launch {
                _errors.emit("Failed to stop advertising: ${e.localizedMessage}")
            }
        }
    }


    override fun startScan() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
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
    }

    private var gattServer: BluetoothGattServer? = null
    private var gattServerCallback: BluetoothGattServerCallback? = null
    private var currentGatt: BluetoothGatt? = null

    private val serviceUUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
    private val messageCharUUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b")

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        gatt.discoverServices()
                        _isConnected.value = true
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _isConnected.value = false
                        closeConnection()
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == messageCharUUID) {
                val message = String(value, Charset.defaultCharset())
                // Handle received message
            }
        }
    }

    override fun startServer(): Flow<ConnectionState> = flow {
        emit(ConnectionState.Connecting)

        gattServerCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: android.bluetooth.BluetoothDevice?,
                status: Int,
                newState: Int
            ) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _isConnected.value = true
                        viewModelScope.launch {
                            _connectionState.emit(ConnectionState.Connected)
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _isConnected.value = false
                        viewModelScope.launch {
                            _connectionState.emit(ConnectionState.Disconnected)
                        }
                    }
                }
            }

            override fun onCharacteristicWriteRequest(
                device: android.bluetooth.BluetoothDevice?,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                if (characteristic?.uuid == messageCharUUID) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                    val message = value?.let { String(it, Charset.defaultCharset()) }
                    // Handle received message
                }
            }
        }

    }

    override fun connectToDevice(device: BluetoothDevice): Flow<ConnectionState> {
        viewModelScope.launch {
            _connectionState.emit(ConnectionState.Connecting)
        }

        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        currentGatt = bluetoothDevice?.connectGatt(context, false, gattCallback)

        if (currentGatt == null) {
            viewModelScope.launch {
                _connectionState.emit(ConnectionState.Disconnected)
            }
        }

        return _connectionState
    }

    override suspend fun sendMessage(message: String): Boolean {
        val gattService = currentGatt?.getService(serviceUUID)
        val characteristic = gattService?.getCharacteristic(messageCharUUID)

        return if (characteristic != null) {
            characteristic.setValue(message.toByteArray(Charset.defaultCharset()))
            currentGatt?.writeCharacteristic(characteristic) == true
        } else false
    }

    override fun closeConnection() {
        currentGatt?.close()
        currentGatt = null
        gattServer?.close()
        gattServer = null
        _isConnected.value = false
    }

    override fun release() {
        closeConnection()
    }
}