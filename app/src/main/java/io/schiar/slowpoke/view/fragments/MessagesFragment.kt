package io.schiar.slowpoke.view.fragments

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.schiar.slowpoke.MainActivity
import io.schiar.slowpoke.R
import io.schiar.slowpoke.view.*
import io.schiar.slowpoke.view.bluetooth.BluetoothService
import io.schiar.slowpoke.view.listeners.OnLastMessagesListener
import io.schiar.slowpoke.view.listeners.OnMessageClickedListener
import io.schiar.slowpoke.view.viewdata.MessageViewData
import io.schiar.slowpoke.viewmodel.MessagesViewModel

class MessagesFragment :
    Fragment(),
    View.OnClickListener,
    OnLastMessagesListener,
    OnMessageClickedListener
{

    private lateinit var viewModel: MessagesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)
        val emptyMessagesView = view.findViewById<ConstraintLayout>(R.id.empty_messages)
        val messagesView = view.findViewById<RecyclerView>(R.id.messages)
        viewModel = ViewModelProvider(requireActivity())[MessagesViewModel::class.java]
        viewModel.messages.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                emptyMessagesView.visibility = View.GONE
                messagesView.visibility = View.VISIBLE
            }
            messagesView.adapter = MessagesAdapter(it, this)
        }
        if (messagesView.adapter == null || messagesView.adapter?.itemCount == 0) {
            emptyMessagesView.visibility = View.VISIBLE
            messagesView.visibility = View.GONE
        }
        view.findViewById<FloatingActionButton>(R.id.search_for_devices).setOnClickListener(this)
        registerResultReceiver()
        return view
    }

    private fun registerResultReceiver() {
        val serviceIntent = Intent(requireActivity(), BluetoothService::class.java)
        val resultReceiver = (requireActivity() as MainActivity).resultReceiver
        resultReceiver.addOnLastMessagesListener(this)
        serviceIntent.putExtra("resultReceiver", resultReceiver)
        requireActivity().startService(serviceIntent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Toast.makeText(requireContext(), "searching for last messages...", Toast.LENGTH_LONG).show()
        sendBluetoothServiceAction(Action.LAST_MESSAGES)
    }

    override fun onClick(p0: View?) {
        findNavController().navigate(R.id.action_messagesFragment_to_devicesFragment)
    }

    override fun onLastMessages(messages: Map<BluetoothDevice, List<MessageViewData>>) {
        viewModel.newLastMessagesWasAdded(messages.mapKeys { it.key.toDeviceViewData() })
    }

    override fun onMessageClick(address: String) {
        viewModel.messageWasClicked(address)
        findNavController().navigate(R.id.action_messagesFragment_to_conversationFragment)
    }
}