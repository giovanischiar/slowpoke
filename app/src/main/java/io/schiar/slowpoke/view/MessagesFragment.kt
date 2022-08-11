package io.schiar.slowpoke.view

import android.content.Intent
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
import io.schiar.slowpoke.view.listeners.OnMessageReceivedListener
import io.schiar.slowpoke.view.listeners.OnMessageSentListener
import io.schiar.slowpoke.view.viewdata.MessageViewData
import io.schiar.slowpoke.viewmodel.MessagesViewModel

class MessagesFragment :
    Fragment(),
    Observer<List<MessageViewData>>,
    View.OnClickListener,
    OnMessageReceivedListener,
    OnMessageSentListener
{
    private lateinit var viewModel: MessagesViewModel

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

    fun retrieveLastMessages() {
        sendServiceAction(Action.LAST_MESSAGES)
    }

    override fun onChanged(messagesViewData: List<MessageViewData>?) {
        messagesViewData ?: return
        val messageHistoryView = requireView().findViewById<MessageHistoryView>(R.id.message_history)
        messageHistoryView.removeAllViews()
        messagesViewData.forEach {
            val (sent, msg) = it
            messageHistoryView.addMessage(sent, msg)
            scrollScrollViewToEnd()
        }
    }

    private fun scrollScrollViewToEnd() {
        val scrollView = requireView().findViewById<ScrollView>(R.id.message_history_scroll_view)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onClick(p0: View?) {
        val msg = requireView().findViewById<EditText>(R.id.message_input).text.toString()
        requireView().findViewById<EditText>(R.id.message_input).text.clear()
        viewModel.onMessageSend(msg)
        val payload = Bundle().apply {
            putString("msg", msg)
        }
        sendServiceActionWithPayload(Action.SEND_MESSAGE, payload)
    }

    private fun sendServiceAction(action: Action) {
        val serviceIntent = Intent(requireActivity(), BluetoothService::class.java)
        serviceIntent.action = action.name
        requireActivity().startService(serviceIntent)
    }

    private fun sendServiceActionWithPayload(action: Action, payload: Bundle) {
        val serviceIntent = Intent(requireActivity(), BluetoothService::class.java)
        serviceIntent.action = action.name
        serviceIntent.putExtras(payload)
        requireActivity().startService(serviceIntent)
    }

    override fun onMessageReceive(msg: String) {
        viewModel.onMessageReceive(msg)
    }

    override fun onMessageSend(msg: String) {
        viewModel.onMessageSend(msg)
    }
}