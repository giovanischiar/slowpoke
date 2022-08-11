package io.schiar.slowpoke.view

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.schiar.slowpoke.Permission
import io.schiar.slowpoke.R
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
    private lateinit var permissionManager: PermissionManager
    private val devices = mutableMapOf<String, BluetoothDevice>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        uuid = UUID.fromString(resources.getString(R.string.uuid))
        viewModel = ViewModelProvider(requireActivity())[MessagesViewModel::class.java]
        permissionManager = PermissionManager(requireActivity())
        val view = inflater.inflate(R.layout.fragment_devices, container, false)

        val serviceIntent = Intent(requireActivity(), BluetoothService::class.java)
        val bluetoothResultReceiver = BluetoothResultReceiver(Handler())
        val messagesFragment = requireActivity()
            .supportFragmentManager
            .findFragmentById(R.id.messages_fragment) as MessagesFragment
        bluetoothResultReceiver.setListeners(this)
        bluetoothResultReceiver.setMessageListeners(messagesFragment)
        serviceIntent.putExtra("onDeviceFoundResultReceiver", bluetoothResultReceiver)
        requireActivity().startService(serviceIntent)
        permissionManager.setPermissionListener(Permission.ACCESS_FINE_LOCATION) {
            permissionManager.setPermissionListener(Permission.BLUETOOTH) {
                sendServiceAction(Action.START_BLUETOOTH_SERVER)
            }
        }
        setObservers(view)
        setListeners(view)
        return view
    }

    private fun sendServiceAction(action: Action) {
        val serviceIntent = Intent(requireActivity(), BluetoothService::class.java)
        serviceIntent.action = action.name
        requireActivity().startService(serviceIntent)
    }

    private fun sendServiceActionWithPayload(action: Action, payload: Bundle) {
        val serviceIntent = Intent(requireActivity(), BluetoothService::class.java)
        serviceIntent.action = action.name
        serviceIntent.putExtras(payload)
        requireActivity().startService(serviceIntent)
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
            sendServiceActionWithPayload(Action.CONNECT_DEVICE, payload)
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
            sendServiceAction(Action.START_BLUETOOTH_DISCOVERY)
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