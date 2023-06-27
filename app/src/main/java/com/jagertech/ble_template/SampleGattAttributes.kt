package com.jagertech.ble_template

object SampleGattAttributes {
    private val attributes: HashMap<String, String> = HashMap()

    init {
        // Sample Services.
        attributes["4fafc201-1fb5-459e-8fcc-c5c9c331914b"] = "Custom Service"
        // Sample Characteristics.
        attributes["6e400003-b5a3-f393-e0a9-e50e24dcca9e"] = "Custom Characteristic"
    }

    fun lookup(uuid: String, defaultName: String): String {
        val name = attributes[uuid]
        return name ?: defaultName
    }
}