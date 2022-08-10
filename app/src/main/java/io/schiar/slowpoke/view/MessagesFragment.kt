package io.schiar.slowpoke.view

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.schiar.slowpoke.R
import io.schiar.slowpoke.view.bluetooth.BluetoothCommunicator
import io.schiar.slowpoke.view.viewdata.MessageViewData
import io.schiar.slowpoke.viewmodel.MessagesViewModel

class MessagesFragment :
    Fragment(),
    Observer<MessageViewData>,
    View.OnClickListener
{
    private lateinit var viewModel: MessagesViewModel
    private var bluetoothCommunicator: BluetoothCommunicator? = null
    private var lastAdded = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity())[MessagesViewModel::class.java]
        viewModel.newMessageViewData.observe(viewLifecycleOwner, this)
        viewModel.remoteDevice.observe(viewLifecycleOwner) {
            requireActivity().title = it.name
        }
        val view = inflater.inflate(R.layout.fragment_messages, container, false)
        view.findViewById<Button>(R.id.send_message).setOnClickListener(this)
        return view
    }

    override fun onStop() {
        super.onStop()
        bluetoothCommunicator?.cancel()
    }

    fun registerBluetoothCommunicator(bluetoothCommunicator: BluetoothCommunicator) {
        this.bluetoothCommunicator = bluetoothCommunicator
    }

    override fun onChanged(messageViewData: MessageViewData?) {
        messageViewData ?: return
        val (sent, msg) = messageViewData
        addMessageToHistory(sent, msg)
        scrollScrollViewToEnd()
    }

    private fun scrollScrollViewToEnd() {
        val scrollView = requireView().findViewById<ScrollView>(R.id.message_history_scroll_view)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addMessageToHistory(sent: Boolean, msg: String) {
        val messageHistory = requireView().findViewById<LinearLayout>(R.id.message_history)
        val messageBalloon = layoutInflater.inflate(
            if (sent) R.layout.message_sent_balloon else R.layout.message_received_balloon,
            messageHistory, false
        ) as FrameLayout
        val textView = layoutInflater.inflate(
            R.layout.message_content,
            messageBalloon,
            false
        ) as TextView
        textView.text = msg
        messageBalloon.layoutParams = messageBalloonLayoutParams(messageBalloon, sent)
        messageBalloon.addView(textView)
        messageHistory.addView(messageBalloon)
        lastAdded = if (sent) 0 else 1
    }

    private fun messageBalloonLayoutParams(
        messageBalloon: FrameLayout,
        sent: Boolean
    ): LinearLayout.LayoutParams {
        val newLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val layoutParams = (messageBalloon.layoutParams ?: newLayoutParams) as LinearLayout.LayoutParams

        val leftMargin = if (sent) 90 else 20
        val rightMargin = if (sent) 20 else 90
        val topMargin = if (lastAdded == -1) {
            0
        } else {
            if (sent && lastAdded == 0 || !sent && lastAdded == 1) {
                10
            } else {
                30
            }
        }

        layoutParams.setMargins(leftMargin, topMargin, rightMargin, 0)
        layoutParams.gravity = if(sent) Gravity.END else Gravity.START
        return layoutParams
    }

    override fun onClick(p0: View?) {
        val msg = requireView().findViewById<EditText>(R.id.message_input).text.toString()
        requireView().findViewById<EditText>(R.id.message_input).text.clear()
        viewModel.onMessageSent(msg)
        bluetoothCommunicator?.onMessageSent(msg)
    }
}