package io.schiar.slowpoke.view.listeners

import android.bluetooth.BluetoothSocket

interface OnBluetoothSocketReceivedListener {
    fun onBluetoothSocketReceived(bluetoothSocket: BluetoothSocket)
}
