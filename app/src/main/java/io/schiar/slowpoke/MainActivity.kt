package io.schiar.slowpoke

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import io.schiar.slowpoke.view.DevicesFragment
import io.schiar.slowpoke.view.MessagesFragment
import io.schiar.slowpoke.viewmodel.MessagesViewModel

class MainActivity: AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModel: MessagesViewModel

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this)[MessagesViewModel::class.java]
        val deviceParcelable = intent.getParcelableExtra("device") as Parcelable?
        if (deviceParcelable != null) {
            setViewModelRemoteDeviceFromService(deviceParcelable as BluetoothDevice)
        }
        viewModel.remoteDevice.observe(this) {
            Toast.makeText(this, "Connection established!", Toast.LENGTH_SHORT).show()
            findViewById<FrameLayout>(R.id.connections).visibility = View.GONE
            findViewById<FrameLayout>(R.id.messages_fragment_layout).visibility = View.VISIBLE
            (supportFragmentManager.findFragmentById(R.id.messages_fragment) as MessagesFragment)
                .retrieveLastMessages()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setViewModelRemoteDeviceFromService(device: BluetoothDevice) {
        (supportFragmentManager.findFragmentById(R.id.devices_fragment) as DevicesFragment)
            .onDeviceConnect(device)
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
}