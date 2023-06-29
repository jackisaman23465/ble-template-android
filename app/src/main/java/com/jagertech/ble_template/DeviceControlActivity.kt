package com.jagertech.ble_template

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.VISIBLE
import android.widget.*
import android.widget.ExpandableListView.OnChildClickListener
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.RangeSlider
import com.jagertech.ble_template.SampleGattAttributes.Brightness_Characteristic_UUID
import com.jagertech.ble_template.SampleGattAttributes.LED_SERVICE_UUID
import com.jagertech.ble_template.SampleGattAttributes.Mode_Characteristic_UUID
import com.jagertech.ble_template.SampleGattAttributes.Switch_Characteristic_UUID
import com.jagertech.ble_template.SampleGattAttributes.lookup
import com.jagertech.ble_template.databinding.GattServicesCharacteristicsBinding
import com.jagertech.ble_template.service.BluetoothLeService
import com.jagertech.ble_template.service.BluetoothLeService.Companion.EXTRA_DATA
import com.jagertech.ble_template.service.BluetoothLeService.Companion.EXTRA_UUID
import com.jagertech.ble_template.service.BluetoothLeService.LocalBinder
import kotlin.experimental.and
import kotlin.math.roundToInt


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with `BluetoothLeService`, which in turn interacts with the
 * Bluetooth LE API.
 */
class DeviceControlActivity : AppCompatActivity() {
    private lateinit var binding: GattServicesCharacteristicsBinding

    private var deviceName: String? = null
    private var deviceAddress: String? = null

    private var bluetoothLeService: BluetoothLeService? = null
    private var gattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>? = ArrayList()
    private var switchCharacteristic: BluetoothGattCharacteristic? = null
    private var brightnessCharacteristic: BluetoothGattCharacteristic? = null
    private var modeCharacteristic: BluetoothGattCharacteristic? = null
    private var connected = false
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    //藍芽服務連線後回調函式
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            bluetoothLeService = (service as LocalBinder).getService()
            if (!bluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            bluetoothLeService!!.connect(deviceAddress!!)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothLeService = null
        }
    }

    //gatt 廣播
    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) { //gatt 連線廣播
                connected = true
                updateConnectionState(R.string.connected)
                invalidateOptionsMenu()
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) { //gatt 斷線廣播
                connected = false
                updateConnectionState(R.string.disconnected)
                invalidateOptionsMenu()
                clearUI()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) { //gatt services 掃描廣播
                val gattServices = bluetoothLeService!!.getSupportedGattServices()
                if(gattServices!=null){
                    var ledService: BluetoothGattService? = null
                    for (gattService in gattServices) {
                        val serviceUuid = gattService.uuid.toString()
                        if (serviceUuid == LED_SERVICE_UUID) {
                            ledService = gattService
                            break
                        }
                    }
                    // 找到特定led service 則顯示led control panel
                    if(ledService != null){
                        displayLedControlPanel(ledService)
                    }else {
                        displayGattServices(gattServices)
                    }
                }
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY == action) { //gatt notify 廣播
                displayData(intent.getStringExtra(EXTRA_DATA))
            } else if (BluetoothLeService.ACTION_DATA_READ == action) { //gatt read 廣播
                val data = intent.getByteArrayExtra(EXTRA_DATA)
                val firstControlByte = data?.get(0)?.toUByte() ?: (0x00).toUByte()
                when (intent.getStringExtra(EXTRA_UUID)) {
                    Switch_Characteristic_UUID -> {
                        bluetoothLeService?.readCharacteristic(brightnessCharacteristic!!)
                        if (firstControlByte == (0x00).toUByte()) {
                            binding.btnState.text = getString(R.string.led_on)
                        } else if (firstControlByte == (0x01).toUByte()) {
                            binding.btnState.text = getString(R.string.led_off)
                        }
                    }
                    Brightness_Characteristic_UUID -> {
                        bluetoothLeService?.readCharacteristic(modeCharacteristic!!)
                        binding.sliderBrightness.setValues(firstControlByte.toFloat())
                    }
                    Mode_Characteristic_UUID -> {
                        binding.spinnerMode.setSelection(firstControlByte.toInt() - 1)
                    }
                }
            } else if (BluetoothLeService.ACTION_DATA_WRITE == action) { //gatt write 廣播

            }
        }
    }

    private val servicesListClickListener =
        OnChildClickListener { parent, v, groupPosition, childPosition, id ->
            if (gattCharacteristics != null) {
                val characteristic = gattCharacteristics!![groupPosition][childPosition]
                val charaProp = characteristic.properties
                if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (notifyCharacteristic != null) {
                        bluetoothLeService!!.setCharacteristicNotification(
                            notifyCharacteristic!!, false
                        )
                        notifyCharacteristic = null
                    }
                    bluetoothLeService!!.readCharacteristic(characteristic)
                }
                if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                    notifyCharacteristic = characteristic
                    bluetoothLeService!!.setCharacteristicNotification(
                        characteristic, true
                    )
                }
                return@OnChildClickListener true
            }
            false
        }

    private fun clearUI() {
        binding.gattServicesList.setAdapter(null as SimpleExpandableListAdapter?)
        binding.dataValue.setText(R.string.no_data)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        binding = GattServicesCharacteristicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.deviceAddress.text = deviceAddress
        binding.gattServicesList.setOnChildClickListener(servicesListClickListener)

        setSupportActionBar(binding.toolbar)

        binding.toolbar.title = deviceName

        binding.btnState.setOnClickListener {
            if (switchCharacteristic != null) {
                if (binding.btnState.text == getString(R.string.led_off)) {
                    binding.btnState.text = getString(R.string.led_on)
                    bluetoothLeService?.writeCharacteristic(switchCharacteristic!!, byteArrayOf(0x00))
                } else {
                    binding.btnState.text = getString(R.string.led_off)
                    bluetoothLeService?.writeCharacteristic(switchCharacteristic!!, byteArrayOf(0x01))
                }
            }
        }

        binding.sliderBrightness.stepSize = 1.0f
        binding.sliderBrightness.addOnChangeListener(RangeSlider.OnChangeListener { slider, value, fromUser ->
            if (brightnessCharacteristic != null) {
                bluetoothLeService?.writeCharacteristic(brightnessCharacteristic!!, byteArrayOf(value.roundToInt().toByte()))
            }
        })

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
        adapter.add("Normal")
        adapter.add("Breathing")
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (modeCharacteristic != null) {
                    when (parent.getItemAtPosition(position).toString()) {
                        "Normal" -> {
                            bluetoothLeService?.writeCharacteristic(modeCharacteristic!!, byteArrayOf(0x01))
                        }
                        "Breathing" -> {
                            bluetoothLeService?.writeCharacteristic(modeCharacteristic!!, byteArrayOf(0x02))
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.spinnerMode.adapter = adapter

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bluetoothLeService != null) {
            val result = bluetoothLeService!!.connect(deviceAddress!!)
            Log.d(TAG, "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        bluetoothLeService = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gatt_services, menu)
        if (connected) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                bluetoothLeService!!.connect(deviceAddress!!)
                return true
            }
            R.id.menu_disconnect -> {
                bluetoothLeService!!.disconnect()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread { binding.connectionState.setText(resourceId) }
    }

    private fun displayData(data: String?) {
        if (data != null) {
            binding.dataValue.text = data
        }
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        val unknownServiceString = resources.getString(R.string.unknown_service)
        val unknownCharaString = resources.getString(R.string.unknown_characteristic)
        val gattServiceData = ArrayList<HashMap<String, String?>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String?>>>()
        gattCharacteristics = ArrayList()

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String?>()
            uuid = gattService.uuid.toString()
            currentServiceData[LIST_NAME] = lookup(uuid, unknownServiceString)
            currentServiceData[LIST_UUID] = uuid
            gattServiceData.add(currentServiceData)
            val gattCharacteristicGroupData = ArrayList<HashMap<String, String?>>()
            val gattCharacteristics = gattService.characteristics
            val charas = ArrayList<BluetoothGattCharacteristic>()
            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                charas.add(gattCharacteristic)
                val currentCharaData = HashMap<String, String?>()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData[LIST_NAME] = lookup(uuid, unknownCharaString)
                currentCharaData[LIST_UUID] = uuid
                gattCharacteristicGroupData.add(currentCharaData)
            }
            this.gattCharacteristics!!.add(charas)
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }
        val gattServiceAdapter = SimpleExpandableListAdapter(
            this,
            gattServiceData,
            android.R.layout.simple_expandable_list_item_2, arrayOf(LIST_NAME, LIST_UUID), intArrayOf(android.R.id.text1, android.R.id.text2),
            gattCharacteristicData,
            android.R.layout.simple_expandable_list_item_2, arrayOf(LIST_NAME, LIST_UUID), intArrayOf(android.R.id.text1, android.R.id.text2),
        )
        binding.gattServicesList.setAdapter(gattServiceAdapter)
    }

    private fun displayLedControlPanel(ledService: BluetoothGattService) {
        val ledCharacteristics = ledService.characteristics
        binding.layout4.visibility = VISIBLE

        for (ledCharacteristic in ledCharacteristics) {
            when (ledCharacteristic.uuid.toString()) {
                Switch_Characteristic_UUID -> {
                    switchCharacteristic = ledCharacteristic
                    bluetoothLeService?.readCharacteristic(switchCharacteristic!!)
                }
                Brightness_Characteristic_UUID -> {
                    brightnessCharacteristic = ledCharacteristic
                    bluetoothLeService?.readCharacteristic(brightnessCharacteristic!!)
                }
                Mode_Characteristic_UUID -> {
                    modeCharacteristic = ledCharacteristic
                    bluetoothLeService?.readCharacteristic(modeCharacteristic!!)
                }
            }
        }
    }

    companion object {
        private val TAG = DeviceControlActivity::class.java.simpleName
        private const val LIST_NAME = "NAME"
        private const val LIST_UUID = "UUID"
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_READ)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE)
            return intentFilter
        }
    }

}