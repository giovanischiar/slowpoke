package io.schiar.slowpoke.view.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.Serializable
import java.util.*

@SuppressLint("MissingPermission")
class BluetoothServer(
    private val uuid: UUID,
    private var bluetoothAdapter: BluetoothAdapter,
    private val bluetoothCommunicator: BluetoothCommunicator,
) : Thread(), Serializable {
    private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        println("listenUsingInsecureRfcommWithServiceRecord $uuid")
        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Slowpoke", uuid)
    }

    override fun run() {
        println("start listening")
        var shouldLoop = true
        while (shouldLoop) {
            val socket: BluetoothSocket? = try {
                mmServerSocket?.accept()
            } catch (e: IOException) {
                println("slowpoke: Socket's accept() method failed $e")
                shouldLoop = false
                null
            }

            socket?.also {
                bluetoothCommunicator.onBluetoothSocketReceived(it)
                mmServerSocket?.close()
                shouldLoop = false
            }
        }
    }

    override fun start() {
        if (state == State.NEW) {
            super.start()
        }
    }

    fun cancel() {
        try {
            mmServerSocket?.close()
        } catch (e: IOException) {
            println("slowpoke: Could not close the connect socket $e")
        }
    }
}