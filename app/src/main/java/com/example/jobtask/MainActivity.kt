package com.example.jobtask

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.jobtask.databinding.ActivityMainBinding
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var hasLocationPermission = false
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var bluetoothManager: BluetoothManager
    private var pairedDevices: Set<BluetoothDevice>? = null
    private var deviceListView: ListView? = null

    private val REQUEST_ENABLE_BT = 1

    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val discoveryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            Log.i("TAG", "onReceive $action")
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                Log.i("TAG", "Discovery finished, hide loading")

            } else if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Toast.makeText(this@MainActivity, "Bluetooth", Toast.LENGTH_SHORT).show()

                Log.i("TAG", "Device Address:" + (device?.address ?: ""))
            }
        }
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = BluetoothManager.getInstance()

        deviceListView = findViewById(R.id.deviceListView)

        binding.swipe.setOnRefreshListener {
            showPairedDevices()

        }

        // Check Bluetooth support
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        // Check Bluetooth permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetoothPermissions()
        }


        binding.search.setOnClickListener {
            if (bluetoothAdapter == null) {
                initBluetoothDiscovery()
            }
            startDiscovery()
        }

        getLocation()

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getLocation() {

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val bluetoothPermission = permissions[Manifest.permission.BLUETOOTH] ?: false
                val bluetoothAdminPermission =
                    permissions[Manifest.permission.BLUETOOTH_ADMIN] ?: false
                val locationPermission =
                    permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

                hasLocationPermission =
                    locationPermission && bluetoothPermission && bluetoothAdminPermission

                if (hasLocationPermission) {
                    if (bluetoothManager.isBluetoothEnabled()) {
                        showPairedDevices()

                    } else {
                        Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Bluetooth Permissions Not Granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        )
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
            REQUEST_ENABLE_BT
        )
    }

    private fun initBluetoothDiscovery() {

        if (bluetoothAdapter?.isEnabled != true) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT)
                    .show()
                finish()
                return
            }

            val intentFilter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            registerReceiver(discoveryReceiver, intentFilter)

            return
        }


    }


    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        if (bluetoothManager.bluetoothAdapter!!.isDiscovering) {
            Log.i("TAG", "cancel start discovery")
            bluetoothManager.bluetoothAdapter!!.cancelDiscovery()
        }
        Log.i("TAG", "start discovery, show loading")
        bluetoothManager.bluetoothAdapter!!.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.bluetoothAdapter!!.cancelDiscovery();
        unregisterReceiver(discoveryReceiver);
    }


    @SuppressLint("MissingPermission")
    private fun updateDeviceList() {
        val deviceNames = discoveredDevices.map { it.name ?: "Unknown" }.toTypedArray()

        Log.i("TAG", "updateDeviceList:${deviceNames}")

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
        deviceListView!!.adapter = adapter
    }

    @SuppressLint("MissingPermission")
    private fun showPairedDevices() {
        pairedDevices = bluetoothManager.getPairedDevices()

        val deviceNames = pairedDevices?.map { it.name.orEmpty() }?.toTypedArray()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames!!)
        deviceListView?.adapter = adapter

        deviceListView?.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = pairedDevices?.elementAt(position)
            selectedDevice?.let { connectToDevice(it) }

        }

        Handler().postDelayed({
            binding.swipe.isRefreshing = false
        }, 1000)

    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        try {
            val socket: BluetoothSocket = bluetoothManager.connectToDevice(device)
            Log.i("TAG", "connectToDevice:${socket}")
            Toast.makeText(this@MainActivity, "Connected to ${device.name}", Toast.LENGTH_SHORT)
                .show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}