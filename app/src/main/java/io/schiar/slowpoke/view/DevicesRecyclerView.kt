package io.schiar.slowpoke.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import io.schiar.slowpoke.view.listeners.OnDeviceClickedListener
import io.schiar.slowpoke.view.viewdata.DeviceViewData

class DevicesRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) :
    RecyclerView(context, attrs),
    Observer<Map<String, DeviceViewData>>
{
    private var onDeviceClickedListener: OnDeviceClickedListener? = null

    init {
        this.adapter = DevicesAdapter(listOf())
    }

    fun registerOnDeviceClickListener(onDeviceClickedListener: OnDeviceClickedListener) {
        this.onDeviceClickedListener = onDeviceClickedListener
    }

    fun setOnDeviceClickListener(onDeviceClickedListener: OnDeviceClickedListener) {
        this.onDeviceClickedListener = onDeviceClickedListener
    }

    override fun onChanged(map: Map<String, DeviceViewData>?) {
        this.adapter = DevicesAdapter((map ?: mapOf()).values.toList(), onDeviceClickedListener)
        this.post { this.scrollToPosition((map ?: mapOf()).size - 1) }
    }
}