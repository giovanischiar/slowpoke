package io.schiar.slowpoke.view

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.os.ResultReceiver
import io.schiar.slowpoke.view.listeners.OnDeviceConnectedListener
import io.schiar.slowpoke.view.listeners.OnDeviceFoundListener
import io.schiar.slowpoke.view.listeners.OnMessageReceivedListener
import io.schiar.slowpoke.view.listeners.OnMessageSentListener

class BluetoothResultReceiver(handler: Handler?) : ResultReceiver(handler) {
    private var onDeviceFoundListener: OnDeviceFoundListener? = null
    private var onDeviceConnectedListener: OnDeviceConnectedListener? = null
    private var onMessageReceivedListener: OnMessageReceivedListener? = null
    private var onMessageSentListener: OnMessageSentListener? = null

    fun <T> setMessagesFragmentListeners(listener: T) where T: OnMessageReceivedListener, T: OnMessageSentListener {
        this.onMessageReceivedListener = listener
        this.onMessageSentListener = listener
    }

    fun <T> setDeviceFragmentListeners(listener: T) where T: OnDeviceFoundListener, T: OnDeviceConnectedListener {
        this.onDeviceFoundListener = listener
        this.onDeviceConnectedListener = listener
    }

    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
        when (resultData?.getString("interface")) {
            "OnDeviceFoundListener" -> {
                val deviceParcelable = resultData.getParcelable("device") as Parcelable?
                if (deviceParcelable != null) {
                    val bondByte = resultData.getByte("bond")
                    val device = deviceParcelable as BluetoothDevice
                    val bond = bondByte.toInt() == 1
                    onDeviceFoundListener?.onDeviceFind(device, bond)
                }
            }

            "OnDeviceConnectedListener" -> {
                val deviceParcelable = resultData.getParcelable("device") as Parcelable?
                if (deviceParcelable != null) {
                    val device = deviceParcelable as BluetoothDevice
                    onDeviceConnectedListener?.onDeviceConnect(device)
                }
            }

            "OnMessageReceivedListener"-> {
                val msg = resultData.getString("msg")
                if (msg != null) {
                    onMessageReceivedListener?.onMessageReceive(msg)
                }
            }

            "OnMessageSentListener" -> {
                val msg = resultData.getString("msg")
                if (msg != null) {
                    onMessageSentListener?.onMessageSend(msg)
                }
            }
        }
    }
}