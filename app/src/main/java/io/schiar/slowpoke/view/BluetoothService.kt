package io.schiar.slowpoke.view

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.os.ResultReceiver
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.schiar.slowpoke.R
import io.schiar.slowpoke.view.bluetooth.BluetoothBroadcastReceiver
import io.schiar.slowpoke.view.bluetooth.BluetoothClient
import io.schiar.slowpoke.view.bluetooth.BluetoothCommunicator
import io.schiar.slowpoke.view.bluetooth.BluetoothServer
import io.schiar.slowpoke.view.listeners.OnDeviceConnectedListener
import io.schiar.slowpoke.view.listeners.OnDeviceFoundListener
import io.schiar.slowpoke.view.listeners.OnMessageReceivedListener
import java.util.*
import kotlin.random.Random

class BluetoothService: Service(),
    OnDeviceFoundListener,
    OnMessageReceivedListener,
    OnDeviceConnectedListener
{
    private lateinit var uuid: UUID
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothBroadcastReceiver: BluetoothBroadcastReceiver
    private lateinit var bluetoothClient: BluetoothClient
    private lateinit var bluetoothCommunicator: BluetoothCommunicator
    private lateinit var bluetoothServer: BluetoothServer

    private var bluetoothResultReceiver: ResultReceiver? = null

    override fun onCreate() {
        createNotificationChannel()
        uuid = UUID.fromString(resources.getString(R.string.uuid))
        val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothBroadcastReceiver = BluetoothBroadcastReceiver(uuid, this)
        bluetoothCommunicator = BluetoothCommunicator(this, this)
        bluetoothClient = BluetoothClient(uuid, bluetoothCommunicator, bluetoothAdapter::cancelDiscovery)
        bluetoothServer = BluetoothServer(uuid, bluetoothAdapter, bluetoothCommunicator)
        registerBroadcastReceiver()

    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val extras = intent?.extras
        val parcelableExtra = extras?.get("onDeviceFoundResultReceiver")
        if (parcelableExtra != null) {
            bluetoothResultReceiver = parcelableExtra as ResultReceiver
            bluetoothAdapter.bondedDevices.map {
                onDeviceFoundListener(it, true)
            }
        }

        when (intent?.action) {
            Action.START_BLUETOOTH_SERVER.name -> bluetoothServer.start()
            Action.START_BLUETOOTH_DISCOVERY.name -> bluetoothAdapter.startDiscovery()
            Action.CONNECT_DEVICE.name -> {
                val device = extras?.getParcelable<BluetoothDevice>("device")
                if (device != null) {
                    bluetoothClient.connectDevice(device)
                }
            }
            Action.SEND_MESSAGE.name -> {
                val msg = extras?.getString("msg")
                if (msg != null) {
                    bluetoothCommunicator.onMessageSent(msg)
                }
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, text: String) {
        val intent = Intent(this, BluetoothService::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val builder = NotificationCompat.Builder(this, "slowpoke")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(this)) {
            notify(Random.nextInt(), builder.build())
        }
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothBroadcastReceiver)
        bluetoothServer.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceConnected(device: BluetoothDevice) {
        val bundle = Bundle().apply {
            putString("interface", "OnDeviceConnectedListener")
            putParcelable("device", device as Parcelable)
        }
        bluetoothResultReceiver?.send(1, bundle)
        println("${device.name} connected lives at ${device.address}")
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceFoundListener(device: BluetoothDevice, bond: Boolean) {
        println("${device.name} found lives at ${device.address}")
        val bundle = Bundle().apply {
            putString("interface", "OnDeviceFoundListener")
            putParcelable("device", device as Parcelable)
            putByte("bond", if (bond) 1 else 0)
        }
        bluetoothResultReceiver?.send(1, bundle)
    }

    override fun onMessageReceived(msg: String) {
        val bundle = Bundle().apply {
            putString("interface", "OnMessageReceivedListener")
            putString("msg", msg)
        }
        bluetoothResultReceiver?.send(1, bundle)
    }

    private fun registerBroadcastReceiver() {
        val bluetoothBroadcastFilter = IntentFilter()
        bluetoothBroadcastFilter.addAction(BluetoothDevice.ACTION_FOUND)
        bluetoothBroadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        bluetoothBroadcastFilter.addAction(BluetoothDevice.ACTION_UUID)
        bluetoothBroadcastFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothBroadcastReceiver, bluetoothBroadcastFilter)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("slowpoke", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}