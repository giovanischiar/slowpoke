package io.schiar.slowpoke.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.schiar.slowpoke.R
import kotlin.math.roundToInt

class MessageHistoryView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private var lastAdded = -1

    private fun generateBalloonLayoutParams(
        messageBalloon: FrameLayout,
        sent: Boolean
    ): LayoutParams {
        val resources = context.resources
        val newLayoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        val layoutParams = (messageBalloon.layoutParams ?: newLayoutParams) as LayoutParams

        val leftMargin = (if (sent) resources.getDimension(R.dimen.message_balloon_end_margin) else resources.getDimension(R.dimen.message_balloon_start_margin)).roundToInt()
        val rightMargin = (if (sent) resources.getDimension(R.dimen.message_balloon_start_margin) else resources.getDimension(R.dimen.message_balloon_end_margin)).roundToInt()
        val topMargin = (if (lastAdded == -1) { 0f } else {
            if (sent && lastAdded == 0 || !sent && lastAdded == 1) resources.getDimension(R.dimen.space_between_same_sender_messages) else resources.getDimension(R.dimen.space_between_different_sender_messages)
        }).roundToInt()

        layoutParams.setMargins(leftMargin, topMargin, rightMargin, 0)
        layoutParams.gravity = if (sent) Gravity.END else Gravity.START
        return layoutParams
    }

    fun addMessage(sent: Boolean, msg: String) {
        val inflater = LayoutInflater.from(context)
        val messageBalloon = inflater.inflate(
            if (sent) R.layout.message_sent_balloon else R.layout.message_received_balloon,
            this, false
        ) as FrameLayout
        val textView = inflater.inflate(
            R.layout.message_content,
            messageBalloon,
            false
        ) as TextView
        textView.text = msg
        messageBalloon.layoutParams = generateBalloonLayoutParams(messageBalloon, sent)
        messageBalloon.addView(textView)
        addView(messageBalloon)
        lastAdded = if (sent) 0 else 1
    }

}