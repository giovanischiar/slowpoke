package io.schiar.slowpoke

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import io.schiar.slowpoke.viewmodel.MessagesViewModel

class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val viewModel = ViewModelProvider(this)[MessagesViewModel::class.java]
        viewModel.remoteDevice.observe(this) {
            Toast.makeText(this, "Connection established!", Toast.LENGTH_SHORT).show()
            findViewById<FrameLayout>(R.id.connections).visibility = View.GONE
            findViewById<FrameLayout>(R.id.messages_fragment_layout).visibility = View.VISIBLE
        }
    }
}