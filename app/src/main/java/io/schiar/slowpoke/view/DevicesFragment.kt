package io.schiar.slowpoke.view

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.schiar.slowpoke.Permission
import io.schiar.slowpoke.R
import io.schiar.slowpoke.view.bluetooth.BluetoothBroadcastReceiver
import io.schiar.slowpoke.view.bluetooth.BluetoothClient
import io.schiar.slowpoke.view.bluetooth.BluetoothCommunicator
import io.schiar.slowpoke.view.bluetooth.BluetoothServer
import io.schiar.slowpoke.view.listeners.OnDeviceClickedListener
import io.schiar.slowpoke.view.listeners.OnDeviceConnectedListener
import io.schiar.slowpoke.view.listeners.OnDeviceFoundListener
import io.schiar.slowpoke.view.viewdata.DeviceViewData
import io.schiar.slowpoke.viewmodel.MessagesViewModel
import java.util.*

class DevicesFragment :
    Fragment(),
    View.OnClickListener,
    OnDeviceClickedListener,
    OnDeviceFoundListener,
    OnDeviceConnectedListener
{
    private lateinit var uuid: UUID
    private lateinit var viewModel: MessagesViewModel
    private lateinit var bluetoothBroadcastReceiver: BluetoothBroadcastReceiver
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothCommunicator: BluetoothCommunicator
    private lateinit var bluetoothClient: BluetoothClient
    private lateinit var bluetoothServer: BluetoothServer
    private lateinit var permissionManager: PermissionManager
    private val devices = mutableMapOf<String, BluetoothDevice>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        uuid = UUID.fromString(resources.getString(R.string.uuid))
        viewModel = ViewModelProvider(requireActivity())[MessagesViewModel::class.java]
        val bluetoothService = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE)
        bluetoothAdapter = (bluetoothService as BluetoothManager).adapter
        bluetoothBroadcastReceiver = BluetoothBroadcastReceiver(uuid, this)
        bluetoothCommunicator = BluetoothCommunicator(viewModel, this)
        permissionManager = PermissionManager(requireActivity())
        bluetoothClient = BluetoothClient(uuid, bluetoothCommunicator, bluetoothAdapter::cancelDiscovery)
        bluetoothServer = BluetoothServer(uuid, bluetoothAdapter, bluetoothCommunicator)
        if (!bluetoothAdapter.isEnabled) {
            permissionManager.setPermissionListener(Permission.ACTION_REQUEST_ENABLE) {
                onBluetoothEnabled()
            }
        } else {
            onBluetoothEnabled()
        }
        val view = inflater.inflate(R.layout.fragment_devices, container, false)
        setObservers(view)
        setListeners(view)
        return view
    }

    @SuppressLint("MissingPermission")
    private fun onBluetoothEnabled() {
        bluetoothServer.start()
        permissionManager.setPermissionListener(Permission.BLUETOOTH_CONNECT) {
            bluetoothAdapter.bondedDevices.map {
                onDeviceFoundListener(it, true)
            }
        }
        registerBroadcastReceiver()
        val fragment = requireActivity()
            .supportFragmentManager
            .findFragmentById(R.id.messages_fragment) as MessagesFragment
        fragment.registerBluetoothCommunicator(bluetoothCommunicator)
    }

    private fun registerBroadcastReceiver() {
        val bluetoothBroadcastFilter = IntentFilter()
        bluetoothBroadcastFilter.addAction(BluetoothDevice.ACTION_FOUND)
        bluetoothBroadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        bluetoothBroadcastFilter.addAction(BluetoothDevice.ACTION_UUID)
        bluetoothBroadcastFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        requireActivity().registerReceiver(bluetoothBroadcastReceiver, bluetoothBroadcastFilter)
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(bluetoothBroadcastReceiver)
        bluetoothServer.cancel()
    }

    private fun setObservers(view: View) {
        viewModel.devices.observe(
            viewLifecycleOwner,
            view.findViewById<DevicesRecyclerView>(R.id.devices_recycler_view)
        )
        viewModel.clientDevice.observe(viewLifecycleOwner, ::onClientDeviceChanged)
    }

    private fun onClientDeviceChanged(clientDevice: DeviceViewData) {
        val device = devices[clientDevice.macAddress]
        if (device == null) {
            Toast.makeText(
                requireActivity(),
                "No such device at $clientDevice",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("connect to ${clientDevice.name}?")
        builder.setPositiveButton("YES") { _, _ ->
            bluetoothClient.connectDevice(device)
        }
        builder.show()
    }

    private fun setListeners(view: View) {
        view.findViewById<DevicesRecyclerView>(R.id.devices_recycler_view)
            .setOnDeviceClickListener(this)
        view.findViewById<Button>(R.id.looking_for_new_devices_btn)
            .setOnClickListener(this)
        view.findViewById<Button>(R.id.be_visible_for_other_devices_btn)
            .setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        when((p0 ?: return).id) {
            R.id.looking_for_new_devices_btn -> onLookingForNewDevicesClicked()
            R.id.be_visible_for_other_devices_btn -> onBeVisibleForNewDevicesClicked()
        }
    }

    @SuppressLint("MissingPermission")
    private fun onLookingForNewDevicesClicked() {
        permissionManager.setPermissionListener(Permission.BLUETOOTH_SCAN) {
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
        }
    }

    private fun onBeVisibleForNewDevicesClicked() {
        permissionManager.setPermissionListener(Permission.BLUETOOTH_ADVERTISE) {
            Toast.makeText(requireContext(), "You're visible!", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceFoundListener(device: BluetoothDevice, bond: Boolean) {
        permissionManager.setPermissionListener(Permission.BLUETOOTH_CONNECT) {
            val name = device.name ?: "(no name)"
            val macAddress = device.address
            devices[macAddress] = device
            val uuids = if (device.uuids != null) {
                device.uuids.map { it.uuid.toString() }
            } else listOf()
            viewModel.addNewDevice(name, macAddress, uuids, bond)
        }
    }

    override fun onDeviceClicked(address: String) {
        viewModel.onDeviceClicked(address)
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceConnected(device: BluetoothDevice) {
        permissionManager.setPermissionListener(Permission.BLUETOOTH_CONNECT) {
            val name = device.name ?: "(no name)"
            val macAddress = device.address
            devices[macAddress] = device
            val uuids = if (device.uuids != null) {
                device.uuids.map { it.uuid.toString() }
            } else listOf()
            viewModel.onRemoteDeviceConnected(name, macAddress, uuids, true)
        }
    }
}