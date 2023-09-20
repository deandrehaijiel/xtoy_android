package com.deandrehaijiel.xtoy.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.deandrehaijiel.xtoy.R
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var messages: List<ChatMessage> = emptyList()
    private lateinit var auth: FirebaseAuth

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.textview_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutResId =
            if (viewType == VIEW_TYPE_SENT) R.layout.message_from else R.layout.message_to
        val itemView = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return MessageViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val currentMessage = messages[position]
        holder.messageTextView.text = currentMessage.message
    }

    override fun getItemCount() = messages.size

    override fun getItemViewType(position: Int): Int {
        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        return if (messages[position].fromId == uid) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    fun updateMessages(updatedMessages: List<ChatMessage>) {
        messages = updatedMessages
        notifyDataSetChanged()
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}
