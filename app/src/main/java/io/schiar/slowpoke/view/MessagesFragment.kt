package io.schiar.slowpoke.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
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
        view.findViewById<TextInputEditText>(R.id.message_input).doOnTextChanged { text, _, _, _ ->
            if (text != null) {
                view.findViewById<Button>(R.id.send_message).isEnabled = text.trim().isNotEmpty()
            }
        }
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
        val messageHistoryView = requireView().findViewById<MessageHistoryView>(R.id.message_history)
        messageHistoryView.addMessage(sent, msg)
        scrollScrollViewToEnd()
    }

    private fun scrollScrollViewToEnd() {
        val scrollView = requireView().findViewById<ScrollView>(R.id.message_history_scroll_view)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onClick(p0: View?) {
        val msg = requireView().findViewById<EditText>(R.id.message_input).text.toString()
        requireView().findViewById<EditText>(R.id.message_input).text.clear()
        viewModel.onMessageSent(msg)
        bluetoothCommunicator?.onMessageSent(msg)
    }
}