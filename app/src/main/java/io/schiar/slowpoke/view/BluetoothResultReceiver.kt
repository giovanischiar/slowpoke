package io.schiar.slowpoke.view

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.os.ResultReceiver
import io.schiar.slowpoke.view.listeners.OnDeviceConnectedListener
import io.schiar.slowpoke.view.listeners.OnDeviceFoundListener
import io.schiar.slowpoke.view.listeners.OnMessageReceivedListener

class BluetoothResultReceiver(handler: Handler?) : ResultReceiver(handler) {
    private var onDeviceFoundResult: OnDeviceFoundListener? = null
    private var onDeviceConnectedListener: OnDeviceConnectedListener? = null
    private var onMessageReceivedListener: OnMessageReceivedListener? = null

    fun <T> setMessageListeners(onMessageReceivedListener: T) where T: OnMessageReceivedListener {
        this.onMessageReceivedListener = onMessageReceivedListener
    }

    fun <T> setListeners(listener: T) where T: OnDeviceFoundListener, T: OnDeviceConnectedListener  {
        this.onDeviceFoundResult = listener
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
                    onDeviceFoundResult?.onDeviceFoundListener(device, bond)
                }
            }

            "OnDeviceConnectedListener" -> {
                val deviceParcelable = resultData.getParcelable("device") as Parcelable?
                if (deviceParcelable != null) {
                    val device = deviceParcelable as BluetoothDevice
                    onDeviceConnectedListener?.onDeviceConnected(device)
                }
            }

            "OnMessageReceivedListener"-> {
                val msg = resultData.getString("msg")
                if (msg != null) {
                    onMessageReceivedListener?.onMessageReceived(msg)
                }
            }
        }
    }
}