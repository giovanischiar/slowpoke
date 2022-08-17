package io.schiar.slowpoke.view.bluetooth

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.os.ResultReceiver
import io.schiar.slowpoke.view.listeners.*
import io.schiar.slowpoke.view.viewdata.MessageViewData

class BluetoothServiceResultReceiver(handler: Handler?) : ResultReceiver(handler) {
    private var onDeviceFoundListeners = listOf<OnDeviceFoundListener>()
    private var onDeviceConnectedListeners = listOf<OnDeviceConnectedListener>()
    private var onMessageReceivedListeners = listOf<OnMessageReceivedListener>()
    private var onMessageSentListeners = listOf<OnMessageSentListener>()
    private var onLastMessagesListeners = listOf<OnLastMessagesListener>()

    fun addOnDeviceFoundListener(onDeviceFoundListener: OnDeviceFoundListener) {
        onDeviceFoundListeners = onDeviceFoundListeners + onDeviceFoundListener
    }

    fun addOnDeviceConnectedListener(OnDeviceConnectedListener: OnDeviceConnectedListener) {
        onDeviceConnectedListeners = onDeviceConnectedListeners + OnDeviceConnectedListener
    }

    fun addOnMessageReceivedListener(onMessageReceivedListener: OnMessageReceivedListener) {
        onMessageReceivedListeners = onMessageReceivedListeners + onMessageReceivedListener
    }

    fun addOnMessageSentListener(onMessageSentListener: OnMessageSentListener) {
        onMessageSentListeners = onMessageSentListeners + onMessageSentListener
    }

    fun addOnLastMessagesListener(onLastMessagesListener: OnLastMessagesListener) {
        onLastMessagesListeners = onLastMessagesListeners + onLastMessagesListener
    }

    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
        when (resultData?.getString("interface")) {
            "OnDeviceFoundListener" -> {
                val deviceParcelable = resultData.getParcelable("device") as Parcelable?
                if (deviceParcelable != null) {
                    val bondByte = resultData.getByte("bond")
                    val device = deviceParcelable as BluetoothDevice
                    val bond = bondByte.toInt() == 1
                    onDeviceFoundListeners.forEach { it.onDeviceFind(device, bond) }
                }
            }

            "OnDeviceConnectedListener" -> {
                val deviceParcelable = resultData.getParcelable("device") as Parcelable?
                if (deviceParcelable != null) {
                    val device = deviceParcelable as BluetoothDevice
                    onDeviceConnectedListeners.forEach { it.onDeviceConnect(device) }
                }
            }

            "OnMessageReceivedListener"-> {
                val msg = resultData.getString("msg")
                if (msg != null) {
                    onMessageReceivedListeners.forEach { it.onMessageReceive(msg) }
                }
            }

            "OnMessageSentListener" -> {
                val msg = resultData.getString("msg")
                if (msg != null) {
                    onMessageSentListeners.forEach { it.onMessageSend(msg) }
                }
            }

            "OnLastMessagesListener" -> {
                val device = resultData.getParcelable("device") as BluetoothDevice?
                val messages = resultData.getParcelableArray("messages")
                if (device != null && messages != null) {
                    onLastMessagesListeners.forEach { it.onLastMessages(
                        mapOf(device to messages.map { it as MessageViewData })
                    ) }
                }
            }
        }
    }
}