package io.schiar.slowpoke.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.schiar.slowpoke.R
import io.schiar.slowpoke.view.listeners.OnDeviceClickedListener
import io.schiar.slowpoke.view.viewdata.DeviceViewData

class DevicesAdapter(
    private var devices: List<DeviceViewData>,
    private var onDeviceClickedListener: OnDeviceClickedListener? = null
)
    : RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val buddyView = inflater.inflate(
            R.layout.device_item_view,
            parent,
            false)
        return ViewHolder(buddyView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devices[position], onDeviceClickedListener)
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bind(deviceViewData: DeviceViewData, onDeviceClickedListener: OnDeviceClickedListener?) {
            deviceViewData.apply {
                itemView.findViewById<TextView>(R.id.device_name).text = name
                itemView.findViewById<TextView>(R.id.device_mac_address).text = macAddress
                val builder: androidx.appcompat.app.AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                builder.setTitle("$name's UUIDs")
                builder.setMessage(uuids.joinToString("\n"))
                itemView.findViewById<Button>(R.id.device_uuids_btn).setOnClickListener {
                    builder.show()
                }
                itemView.findViewById<Button>(R.id.connect_btn).setOnClickListener {
                    onDeviceClickedListener?.onDeviceClicked(macAddress)
                }

                if (uuids.isNotEmpty()) {
                    itemView.findViewById<Button>(R.id.device_uuids_btn).visibility = View.VISIBLE
                    itemView.findViewById<TextView>(R.id.no_uuids).visibility = View.GONE
                }
            }
        }
    }
}