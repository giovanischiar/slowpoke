package io.schiar.slowpoke.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import io.schiar.slowpoke.R
import io.schiar.slowpoke.view.listeners.OnMessageClickedListener
import io.schiar.slowpoke.view.viewdata.DeviceViewData
import io.schiar.slowpoke.view.viewdata.MessageViewData
import java.text.SimpleDateFormat
import java.util.Locale

class MessagesAdapter(
    private val messages: List<Pair<DeviceViewData, MessageViewData>>,
    private val onMessageClick: OnMessageClickedListener
) : Adapter<MessagesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            inflater.inflate(R.layout.messages_item_view, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (deviceViewData, messageViewData) = messages[position]
        holder.bind(deviceViewData, messageViewData, onMessageClick)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bind(
            deviceViewData: DeviceViewData,
            messageViewData: MessageViewData,
            listener: OnMessageClickedListener)
        {
            val (name, address) = deviceViewData
            val (_, content, date) = messageViewData
            itemView.findViewById<TextView>(R.id.contact_name).text = name
            itemView.findViewById<TextView>(R.id.last_message).text = content
            val sdf = SimpleDateFormat("dd-mm-yyyy", Locale.getDefault())
            itemView.findViewById<TextView>(R.id.date).text = sdf.format(date)
            itemView.setOnClickListener { _ -> listener.onMessageClick(address) }
        }
    }
}