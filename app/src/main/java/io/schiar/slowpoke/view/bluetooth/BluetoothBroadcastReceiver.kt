package io.schiar.slowpoke.view.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.schiar.slowpoke.view.listeners.OnDeviceFoundListener
import java.io.Serializable
import java.util.*

class BluetoothBroadcastReceiver(
    private val uuid: UUID,
    private val onDeviceFoundListener: OnDeviceFoundListener
) : BroadcastReceiver(), Serializable {
    private var mDeviceQueue: Queue<BluetoothDevice> = LinkedList()
    constructor() : this(UUID.randomUUID(), object : OnDeviceFoundListener {
        override fun onDeviceFind(device: BluetoothDevice, bond: Boolean) {} }
    )

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = (intent ?: return).action
        when(action) {
            BluetoothDevice.ACTION_UUID -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                onDeviceFoundListener.onDeviceFind(device, false)
                mDeviceQueue.poll()?.fetchUuidsWithSdp()
            }

            ACTION_DISCOVERY_FINISHED -> { mDeviceQueue.poll()?.fetchUuidsWithSdp() }

            BluetoothDevice.ACTION_FOUND -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                onDeviceFoundListener.onDeviceFind(device, false)
                mDeviceQueue.add(device)
            }
        }
    }
}