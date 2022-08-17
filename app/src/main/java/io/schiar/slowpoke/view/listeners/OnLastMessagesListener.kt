package io.schiar.slowpoke.view.listeners

import android.bluetooth.BluetoothDevice
import io.schiar.slowpoke.view.viewdata.MessageViewData

interface OnLastMessagesListener {
    fun onLastMessages(messages: Map<BluetoothDevice, List<MessageViewData>>)
}