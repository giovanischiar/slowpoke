package io.schiar.slowpoke.view.bluetooth

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import io.schiar.slowpoke.MainActivity
import io.schiar.slowpoke.R
import io.schiar.slowpoke.view.Action
import io.schiar.slowpoke.view.listeners.OnDeviceConnectedListener
import io.schiar.slowpoke.view.listeners.OnDeviceFoundListener
import io.schiar.slowpoke.view.listeners.OnMessageReceivedListener
import io.schiar.slowpoke.view.viewdata.MessageViewData
import java.util.*

class BluetoothService: Service(),
    OnDeviceFoundListener,
    OnMessageReceivedListener,
    OnDeviceConnectedListener
{
    companion object { private const val NOTIFICATION_NEW_MESSAGE_ID = 1 }

    private lateinit var uuid: UUID
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothBroadcastReceiver: BluetoothBroadcastReceiver
    private lateinit var bluetoothClient: BluetoothClient
    private lateinit var bluetoothCommunicator: BluetoothCommunicator
    private lateinit var bluetoothServer: BluetoothServer

    private var deviceConnected: BluetoothDevice? = null
    private var lastMessages = listOf<MessageViewData>()
    private var lastNewMessages = listOf<String>()

    private lateinit var sharedPreferences: SharedPreferences

    private var resultReceiver: ResultReceiver? = null

    override fun onCreate() {
        createNotificationChannel()
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
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

        val receiverParcelable = extras?.get("resultReceiver")
        if (receiverParcelable != null) {
            resultReceiver = receiverParcelable as ResultReceiver
        }

        intent?.apply {
            when (action) {
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
                        lastMessages = lastMessages + MessageViewData(true, msg)
                        bluetoothCommunicator.onMessageSend(msg)
                    }
                }
                Action.LAST_MESSAGES.name -> {
                    lastNewMessages = listOf()
                    sendLastMessages()
                }
                Action.BONDED_DEVICES.name -> {
                    bluetoothAdapter.bondedDevices.map {
                        onDeviceFind(it, true)
                    }
                }
                Action.DEVICE_CONNECTED.name -> {
                    bluetoothClient.connectDevice(deviceConnected ?: return@apply)
                }
            }
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, text: String) {
        val messageName = if (lastNewMessages.size == 1) "message" else "messages"

        val pendingIntent = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.messagesFragment)
            .createPendingIntent()

        val builder = NotificationCompat.Builder(this, "slowpoke")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${lastNewMessages.size} $messageName received")
            .setContentText(lastNewMessages.last())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_NEW_MESSAGE_ID, builder.build())
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
    override fun onDeviceConnect(device: BluetoothDevice) {
        val bundle = Bundle().apply {
            putString("interface", "OnDeviceConnectedListener")
            putParcelable("device", device as Parcelable)
        }
        deviceConnected = device
        resultReceiver?.send(1, bundle)
        println("${device.name} connected lives at ${device.address}")
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceFind(device: BluetoothDevice, bond: Boolean) {
        println("${device.name} found lives at ${device.address}")
        val bundle = Bundle().apply {
            putString("interface", "OnDeviceFoundListener")
            putParcelable("device", device as Parcelable)
            putByte("bond", if (bond) 1 else 0)
        }
        resultReceiver?.send(1, bundle)
    }

    private fun receiveMessage(msg: String) {
        val bundle = Bundle().apply {
            putString("interface", "OnMessageReceivedListener")
            putString("msg", msg)
        }
        resultReceiver?.send(1, bundle)
    }

    private fun sendLastMessages() {
        val bundle = Bundle().apply {
            putString("interface", "OnLastMessagesListener")
            putParcelable("device", deviceConnected)
            putParcelableArray("messages", lastMessages.toTypedArray())
        }
        resultReceiver?.send(1, bundle)
    }

    private fun sendMessage(msg: String) {
        val bundle = Bundle().apply {
            putString("interface", "OnMessageSentListener")
            putString("msg", msg)
        }
        resultReceiver?.send(1, bundle)
    }

    override fun onMessageReceive(msg: String) {
        lastMessages = lastMessages + MessageViewData(false, msg)
        val isActivityShowing = sharedPreferences.getBoolean("isActivityShowing", true)
        if (!isActivityShowing) {
            lastNewMessages = lastNewMessages + msg
            showNotification("message received!", msg)
            return
        }
        receiveMessage(msg)
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
            val importance = NotificationManager.IMPORTANCE_HIGH
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