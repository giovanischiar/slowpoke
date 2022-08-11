package io.schiar.slowpoke.view.bluetooth

import android.bluetooth.BluetoothSocket
import io.schiar.slowpoke.view.listeners.OnBluetoothSocketReceivedListener
import io.schiar.slowpoke.view.listeners.OnDeviceConnectedListener
import io.schiar.slowpoke.view.listeners.OnMessageReceivedListener
import io.schiar.slowpoke.view.listeners.OnMessageSentListener
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable

class BluetoothCommunicator(
    private val onMessageReceivedListener: OnMessageReceivedListener,
    private val onDeviceConnectedListener: OnDeviceConnectedListener
)
    : Thread(), OnMessageSentListener, OnBluetoothSocketReceivedListener, Serializable {
    private var mmSocket: BluetoothSocket? = null
    private var mmInStream: InputStream? = null
    private var mmOutStream: OutputStream? = null
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

    override fun run() {
        var numBytes: Int // bytes returned from read()
        while (true) {
            numBytes = try {
                (mmInStream ?: continue ).read(mmBuffer)
            } catch (e: IOException) {
                println("Input stream was disconnected $e")
                break
            }
            onMessageReceivedListener.onMessageReceived(String(mmBuffer, 0, numBytes))
        }
    }

    fun cancel() {
        try {
            mmSocket?.close()
        } catch (e: IOException) {
            println("Could not close the client socket $e")
        }
    }

    override fun onMessageSent(msg: String) {
        val bytes = msg.toByteArray()
        try {
            (mmOutStream ?: return).write(bytes)
        } catch (e: IOException) {
            println("Error occurred when sending data $e")
            return
        }
    }

    override fun onBluetoothSocketReceived(bluetoothSocket: BluetoothSocket) {
        mmSocket = bluetoothSocket
        mmInStream = bluetoothSocket.inputStream
        mmOutStream = bluetoothSocket.outputStream
        onDeviceConnectedListener.onDeviceConnected(bluetoothSocket.remoteDevice)
        this.start()
    }
}