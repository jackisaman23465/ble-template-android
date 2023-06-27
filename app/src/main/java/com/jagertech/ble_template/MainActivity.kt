package com.jagertech.ble_template

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jagertech.ble_template.DeviceControlActivity.Companion.EXTRAS_DEVICE_ADDRESS
import com.jagertech.ble_template.DeviceControlActivity.Companion.EXTRAS_DEVICE_NAME
import com.jagertech.ble_template.adapter.LeDeviceListAdapter
import com.jagertech.ble_template.adapter.OnDeviceClickListener
import com.jagertech.ble_template.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private var TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private var scanning = false
    private val handler = Handler()
    private var connected = false

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    private var deviceAddress: String? = null

    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the
    // ExpandableListView on the UI.

    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner?.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    private val itemClickListener = object : OnDeviceClickListener {
        @SuppressLint("MissingPermission")
        override fun onDeviceClick(device: BluetoothDevice) {
            Log.d(TAG,"click on address:${device.address} item")
            val intent = Intent(this@MainActivity, DeviceControlActivity::class.java)
            intent.putExtra(EXTRAS_DEVICE_NAME,device.name)
            intent.putExtra(EXTRAS_DEVICE_ADDRESS,device.address)
            startActivity(intent)
        }
    }
    private val leDeviceListAdapter = LeDeviceListAdapter(itemClickListener)

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                if(result.device.name != null) {
                    leDeviceListAdapter.addDevice(result.device)
                }
            }
            leDeviceListAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        else{
            requestMultiplePermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val layoutManager = LinearLayoutManager(baseContext)
        binding.recyclerView.layoutManager = layoutManager

        binding.recyclerView.adapter = leDeviceListAdapter
        // 設置刷新監聽器
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            scanLeDevice()
        }

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAnchorView(R.id.fab).setAction("Action", null).show()
        }
    }
}