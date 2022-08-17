package io.schiar.slowpoke.view.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import io.schiar.slowpoke.MainActivity
import io.schiar.slowpoke.Permission
import io.schiar.slowpoke.R
import io.schiar.slowpoke.view.*
import io.schiar.slowpoke.view.bluetooth.BluetoothService
import io.schiar.slowpoke.view.listeners.OnDeviceClickedListener
import io.schiar.slowpoke.view.listeners.OnDeviceConnectedListener
import io.schiar.slowpoke.view.listeners.OnDeviceFoundListener
import io.schiar.slowpoke.view.viewdata.DeviceViewData
import io.schiar.slowpoke.viewmodel.MessagesViewModel

class DevicesFragment :
    Fragment(),
    View.OnClickListener,
    OnDeviceClickedListener,
    OnDeviceFoundListener,
    OnDeviceConnectedListener
{
    private lateinit var viewModel: MessagesViewModel
    private lateinit var permissionManager: PermissionManager
    private val devices = mutableMapOf<String, BluetoothDevice>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity())[MessagesViewModel::class.java]
        permissionManager = PermissionManager(requireActivity())
        val view = inflater.inflate(R.layout.fragment_devices, container, false)
        registerResultReceiver()
        setObservers(view)
        setListeners(view)
        return view
    }

    private fun registerResultReceiver() {
        val serviceIntent = Intent(requireActivity(), BluetoothService::class.java)
        val resultReceiver = (requireActivity() as MainActivity).resultReceiver
        resultReceiver.addOnDeviceFoundListener(this)
        resultReceiver.addOnDeviceConnectedListener(this)
        serviceIntent.putExtra("resultReceiver", resultReceiver)
        requireActivity().startService(serviceIntent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sendBluetoothServiceAction(Action.BONDED_DEVICES)
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
            val payload = Bundle()
            payload.putParcelable("device", device as Parcelable)
            sendBluetoothServiceAction(Action.CONNECT_DEVICE, payload)
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
            sendBluetoothServiceAction(Action.START_BLUETOOTH_DISCOVERY)
        }
    }

    private fun onBeVisibleForNewDevicesClicked() {
        permissionManager.setPermissionListener(Permission.BLUETOOTH_ADVERTISE) {
            Toast.makeText(requireContext(), "You're visible!", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceFind(device: BluetoothDevice, bond: Boolean) {
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
        viewModel.deviceWasClicked(address)
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceConnect(device: BluetoothDevice) {
        permissionManager.setPermissionListener(Permission.BLUETOOTH_CONNECT) {
            val name = device.name ?: "(no name)"
            val macAddress = device.address
            devices[macAddress] = device
            val uuids = if (device.uuids != null) {
                device.uuids.map { it.uuid.toString() }
            } else listOf()
            viewModel.remoteDeviceWasConnected(name, macAddress, uuids, true)
            findNavController().navigate(R.id.action_devicesFragment_to_conversationFragment)
        }
    }
}