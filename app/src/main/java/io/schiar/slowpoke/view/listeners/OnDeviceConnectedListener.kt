package io.schiar.slowpoke.view.listeners

import android.bluetooth.BluetoothDevice

interface OnDeviceConnectedListener {
    fun onDeviceConnect(device: BluetoothDevice)
}
