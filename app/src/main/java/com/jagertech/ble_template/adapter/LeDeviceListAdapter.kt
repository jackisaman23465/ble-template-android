package com.jagertech.ble_template.adapter

import android.bluetooth.BluetoothDevice
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jagertech.ble_template.R

interface OnDeviceClickListener {
    fun onDeviceClick(device: BluetoothDevice)
}

class LeDeviceListAdapter(private val listener: OnDeviceClickListener) : RecyclerView.Adapter<LeDeviceListAdapter.ViewHolder>() {
    private val TAG = "LeDeviceListAdapter"
    private val bleDevices: ArrayList<BluetoothDevice> = arrayListOf()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSSID: TextView = itemView.findViewById(R.id.tvSSID)
        val tvLevel: TextView = itemView.findViewById(R.id.tvLevel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = bleDevices[position]
        try{
            holder.itemView.setOnClickListener {
                listener.onDeviceClick(device)
            }
            holder.tvSSID.text = device.name
            holder.tvLevel.text = device.address
        }catch (e:SecurityException){
            Log.d("onBindViewHolder",e.message.toString())
        }
    }

    override fun getItemCount(): Int {
        return bleDevices.size
    }

    fun addDevice(device: BluetoothDevice?) {
        if (!bleDevices.contains(device)) {
            bleDevices.add(device!!)
        }
    }

    fun clear() {
        bleDevices.clear()
    }
}