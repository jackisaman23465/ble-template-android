package com.jagertech.ble_template

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jagertech.ble_template.DeviceControlActivity.Companion.EXTRAS_DEVICE_ADDRESS
import com.jagertech.ble_template.DeviceControlActivity.Companion.EXTRAS_DEVICE_NAME
import com.jagertech.ble_template.adapter.LeDeviceListAdapter
import com.jagertech.ble_template.adapter.OnDeviceClickListener
import com.jagertech.ble_template.databinding.DeviceScanActivityBinding
import kotlinx.coroutines.*


@SuppressLint("MissingPermission")
class DeviceScanActivity : AppCompatActivity() {
    companion object{
        private val TAG = DeviceScanActivity::class.java.simpleName
    }
    private lateinit var binding: DeviceScanActivityBinding
    private val handler = Handler()

    //是否正在掃描
    private var scanning = false
    //掃描時長
    private val SCAN_PERIOD: Long = 10000
    //藍芽適配器 -> 獲取系統藍芽
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    //藍芽掃描器
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    //recyclerview item click listener
    private val itemClickListener = object : OnDeviceClickListener {
        override fun onDeviceClick(device: BluetoothDevice) {
            //點擊後跳轉至裝置資訊頁面
            Log.d(TAG,"device mac address:${device.address}")
            val intent = Intent(this@DeviceScanActivity, DeviceControlActivity::class.java)
            intent.putExtra(EXTRAS_DEVICE_NAME,device.name)
            intent.putExtra(EXTRAS_DEVICE_ADDRESS,device.address)
            startActivity(intent)
        }
    }

    //recyclerview adapter
    private val leDeviceListAdapter = LeDeviceListAdapter(itemClickListener)

    //藍芽掃描到後回調函式
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                //能索引到名字的才加入清單
                if(result.device.name != null) {
                    leDeviceListAdapter.addDevice(result.device)
                }
            }
            leDeviceListAdapter.notifyDataSetChanged()
        }
    }

    //開始掃描
    private fun startScan() {
        scanning = true
        //超過SCAN_PERIOD時間後停止掃描
        handler.postDelayed({
            stopScan()
        }, SCAN_PERIOD)
//        val filters: MutableList<ScanFilter> = ArrayList()
//        val filter = ScanFilter.Builder()
//            .setDeviceAddress(SampleGattAttributes.sampleMacAddress) // 特定藍牙裝置的 MAC 地址
//            .build()
//        filters.add(filter)
//        val settings = ScanSettings.Builder()
//            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) // 低功耗模式
//            .build()
//        bluetoothLeScanner?.startScan(filters,settings,leScanCallback)
        bluetoothLeScanner?.startScan(leScanCallback)
        invalidateOptionsMenu()
    }

    //停止掃描
    private fun stopScan(){
        scanning = false
        bluetoothLeScanner?.stopScan(leScanCallback)
        invalidateOptionsMenu()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearUI(){
        leDeviceListAdapter.clear()
        leDeviceListAdapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Android Api 31以上要多請求SCAN & CONNECT權限
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

        //UI Setting
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = DeviceScanActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val layoutManager = LinearLayoutManager(baseContext)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = leDeviceListAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.scan_devices, menu)
        if (scanning) {
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_stop).isVisible = true
        } else {
            menu.findItem(R.id.menu_scan).isVisible = true
            menu.findItem(R.id.menu_stop).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                startScan()
                return true
            }
            R.id.menu_stop -> {
                stopScan()
                return true
            }
            R.id.menu_clear -> {
                clearUI()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}