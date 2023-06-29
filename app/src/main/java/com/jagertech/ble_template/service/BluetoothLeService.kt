package com.jagertech.ble_template.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.util.*

@SuppressLint("MissingPermission")
class BluetoothLeService : Service() {
    companion object {
        const val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_READ = "ACTION_DATA_READ"
        const val ACTION_DATA_NOTIFY = "ACTION_DATA_NOTIFY"
        const val ACTION_DATA_WRITE = "ACTION_DATA_WRITE"
        const val EXTRA_DATA = "EXTRA_DATA"
        const val EXTRA_UUID = "EXTRA_UUID"
    }

    private var TAG = "BluetoothLeService"
    private var UUID_NOTIFY_DESCRIPTION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var connectionState = STATE_DISCONNECTED

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == STATE_CONNECTED) {
                // successfully connected to the GATT Server
                broadcastUpdate(ACTION_GATT_CONNECTED)
                connectionState = STATE_CONNECTED
                // Attempts to discover services after successful connection.
                bluetoothGatt?.discoverServices()
            } else if (newState == STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
                connectionState = STATE_DISCONNECTED
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_READ, characteristic)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_WRITE, characteristic)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
//            Log.i(TAG, "onCharacteristicChanged");
            broadcastUpdate(ACTION_DATA_NOTIFY, characteristic)
            val data = characteristic.value
            if (data != null && data.size > 0) {
                Log.d(TAG, "Notification data" + byteToString(data).toString())
            }
        }
    }

    private fun broadcastUpdate(
        action: String,
        characteristic: BluetoothGattCharacteristic? = null
    ) {
        val intent = Intent(action)

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        if (characteristic != null) {
            val data: ByteArray? = characteristic.value
            if (data?.isNotEmpty() == true) {
                val hexString: String = data.joinToString(separator = " ") {
                    String.format("%02X", it)
                }
                Log.d(TAG,hexString)
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(EXTRA_UUID, characteristic.uuid.toString())
            }
        }
        sendBroadcast(intent)
    }

    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        bluetoothGatt?.let { gatt ->
            gatt.setCharacteristicNotification(characteristic, enabled)
            val descriptor =
                characteristic.getDescriptor(UUID_NOTIFY_DESCRIPTION)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService>? {
        return bluetoothGatt?.services
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun initialize(): Boolean {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    fun connect(address: String): Boolean {
        Log.d(TAG,"connect gatt")
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                // connect to the GATT server on the device
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.  Unable to connect.")
                return false
            }
        } ?: run {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
    }

    fun disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        bluetoothGatt?.disconnect()
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.readCharacteristic(characteristic) ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
            return
        }
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, byteValues: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGatt?.writeCharacteristic(characteristic, byteValues, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) ?: run {
                Log.w(TAG, "BluetoothGatt not initialized")
                return
            }
        }else{
            characteristic.value = byteValues
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            bluetoothGatt?.writeCharacteristic(characteristic) ?: run {
                Log.w(TAG, "BluetoothGatt not initialized")
                return
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    private fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    //</editor-fold>
    fun byteToString(data: ByteArray): StringBuilder? {
        val str = StringBuilder()
        str.append("0x")
        for (i in data.indices) str.append(String.format("%02X", data[i]))
        return str
    }
}