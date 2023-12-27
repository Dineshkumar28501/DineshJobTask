package com.example.jobtask

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DiscoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val action = intent.action
        Log.i("TAG", "onReceive $action")
        if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
            Log.i("TAG", "Discovery finished, hide loading")

        } else if (BluetoothDevice.ACTION_FOUND == action) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            Log.i("TAG", "onReceive:${device!!.address} ")
        }
    }
}