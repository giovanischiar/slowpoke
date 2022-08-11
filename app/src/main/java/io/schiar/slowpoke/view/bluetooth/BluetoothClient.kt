package io.schiar.slowpoke.view.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import java.io.Serializable
import java.util.*

class BluetoothClient(
    private val uuid: UUID,
    private val bluetoothCommunicator: BluetoothCommunicator,
    private val cancelDiscovery: () -> Unit,
) : Thread(), Serializable {
    private var device : BluetoothDevice? = null

    @SuppressLint("MissingPermission")
    override fun run() {
        cancelDiscovery()
        println("createRfcommSocketToServiceRecord $uuid")
        val socket = device?.createRfcommSocketToServiceRecord(uuid) ?: return
        try { socket.connect() } catch (e: Exception) { println(e.message) }
        bluetoothCommunicator.onBluetoothSocketReceived(socket)
    }

    fun connectDevice(device: BluetoothDevice) {
        this.device = device
        this.start()
    }
}