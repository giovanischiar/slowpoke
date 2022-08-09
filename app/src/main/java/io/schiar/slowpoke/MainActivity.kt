package io.schiar.slowpoke

import android.Manifest
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.schiar.slowpoke.view.DevicesRecyclerView
import io.schiar.slowpoke.view.MessagesFragment
import io.schiar.slowpoke.view.bluetooth.BluetoothBroadcastReceiver
import io.schiar.slowpoke.view.bluetooth.BluetoothClient
import io.schiar.slowpoke.view.bluetooth.BluetoothCommunicator
import io.schiar.slowpoke.view.bluetooth.BluetoothServer
import io.schiar.slowpoke.view.listeners.OnDeviceClickedListener
import io.schiar.slowpoke.view.listeners.OnDeviceConnectedListener
import io.schiar.slowpoke.view.listeners.OnDeviceFoundListener
import io.schiar.slowpoke.viewmodel.MessagesViewModel
import java.util.*

class MainActivity:
    AppCompatActivity(),
    LifecycleOwner,
    View.OnClickListener,
    Observer<String>,
    OnDeviceFoundListener,
    OnDeviceClickedListener,
    OnDeviceConnectedListener
{
    private lateinit var viewModel: MessagesViewModel
    private val devices = mutableMapOf<String, BluetoothDevice>()

    private lateinit var bluetoothBroadcastReceiver: BluetoothBroadcastReceiver
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothCommunicator: BluetoothCommunicator
    private var bluetoothClient: BluetoothClient? = null
    private var hasPermission = false
    private lateinit var uuid: UUID

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val devicesRecyclerView = findViewById<DevicesRecyclerView>(R.id.devices_recycler_view)

        uuid = UUID.fromString(resources.getString(R.string.uuid))

        viewModel = ViewModelProvider(this)[MessagesViewModel::class.java]
        viewModel.currentScreen.observe(this, this)
        viewModel.devices.observe(this, devicesRecyclerView)
        devicesRecyclerView.registerOnDeviceClickListener(this)
        viewModel.clientDevice.observe(this) {
            val device = devices[it.macAddress]
            if (device == null) {
                Toast.makeText(
                    this@MainActivity,
                    "No such device at $it",
                    Toast.LENGTH_LONG
                ).show()
                return@observe
            }
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("connect to ${it.name}?")
            builder.setPositiveButton("YES") { _, _ ->
                bluetoothClient?.connectDevice(device)
            }
            builder.show()
        }
        devicesRecyclerView.setOnDeviceClickListener(viewModel)

        findViewById<Button>(R.id.looking_for_another_buddy_btn).setOnClickListener(this)
        findViewById<Button>(R.id.be_visible_for_other_possible_buddies_btn).setOnClickListener(this)

        bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter
        bluetoothCommunicator = BluetoothCommunicator(viewModel, this)

        val fragment = supportFragmentManager.findFragmentById(R.id.messages_fragment) as MessagesFragment
        fragment.registerBluetoothCommunicator(bluetoothCommunicator)
        bluetoothBroadcastReceiver = BluetoothBroadcastReceiver(uuid, this)
        val bluetoothBroadcastFilter = IntentFilter()
        bluetoothBroadcastFilter.addAction(BluetoothDevice.ACTION_FOUND)
        bluetoothBroadcastFilter.addAction(ACTION_DISCOVERY_FINISHED)
        bluetoothBroadcastFilter.addAction(BluetoothDevice.ACTION_UUID)
        bluetoothBroadcastFilter.addAction(ACTION_STATE_CHANGED)
        registerReceiver(bluetoothBroadcastReceiver, bluetoothBroadcastFilter)

        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.bondedDevices.toList().forEach { device ->
                onDeviceFoundListener(device, true)
            }
            BluetoothServer(uuid, bluetoothAdapter, bluetoothCommunicator).start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkPermission()) {
                    val permissionsArray = arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    ActivityCompat.requestPermissions(this, permissionsArray, 1)
                }
            } else {
                //if (checkPermission2()) {
                    ActivityCompat.requestPermissions(this, arrayOf(
                        BLUETOOTH,
                        BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ), 1)
                //}
            }
        }
    }

    override fun onStop() {
        super.onStop()
        println("unregister bluetoothBroadcastReceiver...")
        unregisterReceiver(bluetoothBroadcastReceiver)
    }

    @SuppressLint("MissingPermission")
    private fun startLookingForBluetoothDevices() {
        val startDiscovery = bluetoothAdapter.startDiscovery()
        if (startDiscovery) {
            println("Started looking for bluetooth devices!")
            bluetoothClient = BluetoothClient(
                uuid,
                bluetoothCommunicator,
                bluetoothAdapter::cancelDiscovery
            )
        } else {
            println("Couldn't start :/ back to the stackoverflow")
        }
        val bondedDevices =
            bluetoothAdapter.bondedDevices.joinToString("\n") { "${it.name}: ${it.address}" }
        //println("slowpoke: bonded devices: \n$bondedDevices")
        //val bluetoothDevice = BluetoothDevice()
//        val connectThread = ConnectThread(
//            bluetoothDevice,
//            getSystemService(BluetoothManager::class.java).adapter
//        )
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        hasPermission = grantResults.toList()
            .map { it == PackageManager.PERMISSION_GRANTED }
            .reduce { acc, it -> acc && it }
    }

    override fun onClick(p0: View?) {
        when ((p0?: return).id) {
            R.id.looking_for_another_buddy_btn -> {
                if (hasPermission) {
                    startLookingForBluetoothDevices()
                } else {
                    Toast.makeText(this, "No permission :(", Toast.LENGTH_SHORT).show()
                }
            }

            R.id.be_visible_for_other_possible_buddies_btn -> {
                if (hasPermission) {
                    startBeingVisible()
                }
            }
        }
    }

    private fun startBeingVisible() {
        val discoverableIntent = Intent(ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(EXTRA_DISCOVERABLE_DURATION, 300)
        }

        @Suppress("DEPRECATION")
        startActivityForResult(discoverableIntent, 1)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAllPermissions(permissions: List<String>): Boolean {
        return permissions.map {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }.reduce { acc, it -> acc || it }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkPermission(): Boolean {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        return checkAllPermissions(permissions)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermission2(): Boolean {
        return checkAllPermissions(
            listOf(
                BLUETOOTH,
                BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceFoundListener(device: BluetoothDevice, bond: Boolean) {
        val name = device.name ?: "(no name)"
        val macAddress = device.address
        devices[macAddress] = device
        val uuids = if (device.uuids != null) {
            device.uuids.map { it.uuid.toString() }
        } else listOf()
        viewModel.addNewDevice(name, macAddress, uuids, bond)
    }

    override fun onChanged(currentScreen: String?) {
        if (currentScreen === "messages") {
            Toast.makeText(this, "Connection established!", Toast.LENGTH_SHORT).show()
            findViewById<LinearLayout>(R.id.connections).visibility = View.GONE
            findViewById<FrameLayout>(R.id.messages_fragment_layout).visibility = View.VISIBLE
        }
    }

    override fun onDeviceClicked(address: String) {
        viewModel.onDeviceClicked(address)
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceConnected(device: BluetoothDevice) {
        val name = device.name ?: "(no name)"
        val macAddress = device.address
        devices[macAddress] = device
        val uuids = if (device.uuids != null) {
            device.uuids.map { it.uuid.toString() }
        } else listOf()
        viewModel.onRemoteDeviceConnected(name, macAddress, uuids, true)
    }
}