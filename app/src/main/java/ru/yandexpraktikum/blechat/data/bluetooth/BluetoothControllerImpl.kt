package ru.yandexpraktikum.blechat.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
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
import androidx.annotation.RequiresPermission
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.yandexpraktikum.blechat.domain.bluetooth.BluetoothController
import ru.yandexpraktikum.blechat.domain.model.ConnectionState
import ru.yandexpraktikum.blechat.domain.model.Message
import ru.yandexpraktikum.blechat.domain.model.ScannedBluetoothDevice
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

    private val _isBluetoothEnabled = MutableStateFlow(false)
    override val isBluetoothEnabled: StateFlow<Boolean>
        get() = _isBluetoothEnabled.asStateFlow()

    private val _connectionState = MutableSharedFlow<ConnectionState>()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedBluetoothDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<ScannedBluetoothDevice>>
        get() = _scannedDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<ScannedBluetoothDevice>>(emptyList())
    override val connectedDevices: StateFlow<List<ScannedBluetoothDevice>>
        get() = _connectedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    init {
        initializeBluetoothState()
    }

    private fun initializeBluetoothState() {
        try {
            _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        } catch (e: Exception) {
            viewModelScope.launch {
                _errors.emit("Failed to initialize Bluetooth: ${e.localizedMessage}")
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
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
    }

    private var gattServer: BluetoothGattServer? = null
    private var gattServerCallback: BluetoothGattServerCallback? = null
    private var currentGatt: BluetoothGatt? = null

    private val serviceUUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
    private val writeCharUUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b")
    private val notifyCharUUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4c")
    private val CLIENT_CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        gatt.discoverServices()
                        _scannedDevices.update { devices ->
                            devices.map {
                                if (it.address == gatt.device.address) {
                                    it.copy(isConnected = true)
                                } else it
                            }
                        }
                        _isConnected.value = true
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _scannedDevices.update { devices ->
                            devices.map {
                                if (it.address == gatt.device.address) {
                                    it.copy(isConnected = false)
                                } else it
                            }
                        }
                        _isConnected.value = false
                        closeConnection()
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "Descriptor write successful, notifications should now work")
            } else {
                Log.e("BLE", "Descriptor write failed with status: $status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(serviceUUID)
                val notifyCharacteristic = service?.getCharacteristic(notifyCharUUID)
                if (notifyCharacteristic != null) {
                    Log.d("BLE", "Found notify characteristic, enabling notifications")
                    gatt?.setCharacteristicNotification(notifyCharacteristic, true)
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
                                it.copy(messages = it.messages + Message(
                                    text = message,
                                    senderAddress = gatt.device.address,
                                    isFromLocalUser = false
                                ))
                            } else it
                        }
                    }
                }
                Log.i("BLE", "Received message: $message")
            } else {
                Log.i("BLE", "Received message for unknown characteristic")
            }
        }
    }

    override fun startServer() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        gattServerCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: android.bluetooth.BluetoothDevice?,
                status: Int,
                newState: Int
            ) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _isConnected.value = true
                        _connectedDevices.update { devices ->
                            if (devices.none { it.address == device?.address } && device != null) {
                                devices + ScannedBluetoothDevice(
                                    name = device.name,
                                    address = device.address
                                )
                            } else devices
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _connectedDevices.update { devices ->
                            devices - devices.first {it.address == device?.address}
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
                if (characteristic?.uuid == writeCharUUID) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                    val message = value?.let { String(it, Charset.defaultCharset()) }
                    // Handle received message here
                    viewModelScope.launch {
                        message?.let {
                            _connectedDevices.update { devices ->
                                devices.map {
                                    if (it.address == device?.address) {
                                        it.copy(messages = it.messages + Message(
                                            text = message,
                                            senderAddress = device.address,
                                            isFromLocalUser = false
                                        ))
                                    } else it
                                }
                            }
                        }
                    }
                }
            }
        }

        gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)

        // Create and add the GATT service
        val service = BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val writeCharacteristic = BluetoothGattCharacteristic(
            writeCharUUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        val notifyCharacteristic = BluetoothGattCharacteristic(
            notifyCharUUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val descriptor = BluetoothGattDescriptor(
            CLIENT_CONFIG_DESCRIPTOR,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        notifyCharacteristic.addDescriptor(descriptor)

        service.addCharacteristic(writeCharacteristic)
        service.addCharacteristic(notifyCharacteristic)
        gattServer?.addService(service)

        // Start advertising to make the server discoverable
        startAdvertising()
    }

    override fun stopServer() {
        try {
            stopAdvertising()
            gattServer?.close()
            gattServer = null
            gattServerCallback = null
            _isConnected.value = false
            viewModelScope.launch {
                _connectionState.emit(ConnectionState.Disconnected)
            }
            Log.d("BLE", "Server stopped successfully")
        } catch (e: Exception) {
            Log.e("BLE", "Failed to stop server", e)
            viewModelScope.launch {
                _errors.emit("Failed to stop server: ${e.localizedMessage}")
            }
        }
    }

    override fun connectToDevice(device: ScannedBluetoothDevice): Flow<ConnectionState> {
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

    override suspend fun sendServerMessage(message: String, deviceAddress: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        // Get the existing service and characteristic
        val service = gattServer?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(notifyCharUUID)

        return try {
            if (characteristic != null) {
                characteristic.setValue(message.toByteArray(Charset.defaultCharset()))
                val success = gattServer?.notifyCharacteristicChanged(device, characteristic, false)
                Log.i("BLE", "Server message sent: $success")
                _connectedDevices.update { devices ->
                    devices.map {
                        if (it.address == deviceAddress) {
                            it.copy(messages = it.messages + Message(
                                text = message,
                                senderAddress = bluetoothAdapter?.address ?: "",
                                isFromLocalUser = true
                            ))
                        } else it
                    }
                }
                true
            } else {
                Log.e("BLE", "Characteristic not found")
                false
            }
        } catch (e: Exception) {
            Log.e("BLE", "Failed to send server message", e)
            false
        }
    }

    override suspend fun sendMessage(message: String, deviceAddress: String): Boolean {
        val gattService = currentGatt?.getService(serviceUUID)
        val characteristic = gattService?.getCharacteristic(writeCharUUID)

        return if (characteristic != null) {
            characteristic.setValue(message.toByteArray(Charset.defaultCharset()))
            currentGatt?.writeCharacteristic(characteristic)
            _scannedDevices.update { devices ->
                devices.map {
                    if (it.address == deviceAddress) {
                        it.copy(messages = it.messages + Message(
                            text = message,
                            senderAddress = bluetoothAdapter?.address ?: "",
                            isFromLocalUser = true
                        ))
                    } else it
                }
            }
            true
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