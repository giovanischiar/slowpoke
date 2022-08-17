package io.schiar.slowpoke.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import io.schiar.slowpoke.R
import io.schiar.slowpoke.view.*
import io.schiar.slowpoke.view.viewdata.DeviceViewData
import io.schiar.slowpoke.view.viewdata.MessageViewData
import io.schiar.slowpoke.viewmodel.MessagesViewModel

class ConversationFragment :
    Fragment(),
    View.OnClickListener
{
    private lateinit var viewModel: MessagesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_conversation, container, false)
        view.findViewById<Button>(R.id.send_message).setOnClickListener(this)
        view.findViewById<TextInputEditText>(R.id.message_input).doOnTextChanged { text, _, _, _ ->
            if (text != null) {
                view.findViewById<Button>(R.id.send_message).isEnabled = text.trim().isNotEmpty()
            }
        }
        registerResultReceiver()
        return view
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().title = getString(R.string.app_name)
    }

    private fun registerResultReceiver() {
//        val serviceIntent = Intent(requireActivity(), BluetoothService::class.java)
//        val resultReceiver = (requireActivity() as MainActivity).resultReceiver
//        serviceIntent.putExtra("resultReceiver", resultReceiver)
//        requireActivity().startService(serviceIntent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity())[MessagesViewModel::class.java]
        viewModel.remoteDevice.observe(viewLifecycleOwner, ::onRemoteDeviceChanged)
        viewModel.currentConversation.observe(viewLifecycleOwner, ::onDeviceConversationChanged)
    }

    private fun onRemoteDeviceChanged(deviceViewData: DeviceViewData?) {
        deviceViewData ?: return
        requireActivity().title = deviceViewData.name
    }

    private fun onDeviceConversationChanged(conversation: List<MessageViewData>?) {
        conversation ?: return
        val messageHistoryView = requireView().findViewById<MessageHistoryView>(R.id.message_history)
        messageHistoryView.removeAllViews()
        conversation.forEach {
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
        viewModel.messageWasSent(msg)
        val payload = Bundle().apply {
            putString("msg", msg)
        }
        sendBluetoothServiceAction(Action.SEND_MESSAGE, payload)
    }
}