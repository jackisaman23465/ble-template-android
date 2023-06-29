package com.jagertech.ble_template

object SampleGattAttributes {
    val sampleMacAddress = "A8:03:2A:17:85:E6"
    private val attributes: HashMap<String, String> = HashMap()
    const val LED_SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    const val Switch_Characteristic_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
    const val Brightness_Characteristic_UUID = "58761b6e-1575-11ee-be56-0242ac120002"
    const val Mode_Characteristic_UUID = "b9fde1bc-1586-11ee-be56-0242ac120002"
    init {
        attributes["00001800-0000-1000-8000-00805f9b34fb"] = "Generic Access Service"
        attributes["00002a00-0000-1000-8000-00805f9b34fb"] = "Device Name"
        attributes["00002a01-0000-1000-8000-00805f9b34fb"] = "Appearance"
        attributes["00002aa6-0000-1000-8000-00805f9b34fb"] = "Central Address Resolution"

        attributes["00001801-0000-1000-8000-00805f9b34fb"] = "Generic Attribute Service"
        attributes["00002a05-0000-1000-8000-00805f9b34fb"] = "Service Changed"

        // Custom LED Services.
        attributes[LED_SERVICE_UUID] = "LED Service"
        // Custom LED Characteristics.
        attributes[Switch_Characteristic_UUID] = "LED Switch Characteristic"
        attributes[Brightness_Characteristic_UUID] = "LED Brightness Characteristic"
        attributes[Mode_Characteristic_UUID] = "LED Mode Characteristic"
    }

    fun lookup(uuid: String, defaultName: String): String {
        val name = attributes[uuid]
        return name ?: defaultName
    }
}