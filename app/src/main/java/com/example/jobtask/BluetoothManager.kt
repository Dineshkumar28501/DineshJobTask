package com.example.jobtask

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.*

class BluetoothManager private constructor() {

    companion object {
        fun getInstance(): BluetoothManager {
            return BluetoothManager()
        }
    }

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice>? {
        return bluetoothAdapter?.bondedDevices
    }


    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    fun connectToDevice(device: BluetoothDevice): BluetoothSocket {
        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SerialPortService ID
        return device.createRfcommSocketToServiceRecord(uuid)
    }
}
