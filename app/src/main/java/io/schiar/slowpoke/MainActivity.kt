package io.schiar.slowpoke

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import io.schiar.slowpoke.view.Action
import io.schiar.slowpoke.view.bluetooth.BluetoothServiceResultReceiver
import io.schiar.slowpoke.view.bluetooth.BluetoothService
import io.schiar.slowpoke.view.listeners.OnDeviceConnectedListener
import io.schiar.slowpoke.view.listeners.OnMessageReceivedListener
import io.schiar.slowpoke.view.listeners.OnMessageSentListener
import io.schiar.slowpoke.view.sendBluetoothServiceAction
import io.schiar.slowpoke.view.toDeviceViewData
import io.schiar.slowpoke.viewmodel.MessagesViewModel

class MainActivity:
    AppCompatActivity(),
    OnDeviceConnectedListener,
    OnMessageReceivedListener
{
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModel: MessagesViewModel
    lateinit var resultReceiver: BluetoothServiceResultReceiver

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        viewModel = ViewModelProvider(this)[MessagesViewModel::class.java]
        resultReceiver = BluetoothServiceResultReceiver(Handler(Looper.getMainLooper()))
        registerResultReceiver()
        val deviceParcelable = intent.getParcelableExtra("device") as Parcelable?
        sendBluetoothServiceAction(Action.START_BLUETOOTH_SERVER)
    }

    private fun registerResultReceiver() {
        val serviceIntent = Intent(this, BluetoothService::class.java)
        resultReceiver.addOnDeviceConnectedListener(this)
        resultReceiver.addOnMessageReceivedListener(this)
        serviceIntent.putExtra("resultReceiver", resultReceiver)
        startService(serviceIntent)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.edit().apply {
            putBoolean("isActivityShowing", false)
            apply()
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.edit().apply {
            putBoolean("isActivityShowing", true)
            apply()
        }
    }

    override fun onDeviceConnect(device: BluetoothDevice) {
        val (name, macAddress, uuids, bond) = device.toDeviceViewData(bond = true)
        Toast.makeText(this, "$name connected!", Toast.LENGTH_SHORT).show()
        viewModel.remoteDeviceWasConnected(name, macAddress, uuids, bond)
    }

    override fun onMessageReceive(msg: String) {
        viewModel.messageWasReceived(msg)
    }
}