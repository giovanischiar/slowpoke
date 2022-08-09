package io.schiar.slowpoke.view.listeners

import android.bluetooth.BluetoothDevice

interface OnDeviceConnectedListener {
    fun onDeviceConnected(device: BluetoothDevice)
}
