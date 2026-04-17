package com.cunyi.doctor.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cunyi.doctor.databinding.ItemMessageBotBinding
import com.cunyi.doctor.databinding.ItemMessageUserBinding

class ChatAdapter : ListAdapter<MainViewModel.ChatMessage, RecyclerView.ViewHolder>(Diff()) {

    private var streamingResponse: String? = null

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_BOT = 1
        private const val VIEW_TYPE_STREAMING = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < currentList.size) VIEW_TYPE_BOT else VIEW_TYPE_STREAMING
    }

    override fun getItemCount(): Int {
        return currentList.size + if (streamingResponse != null) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val b = ItemMessageUserBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                UserViewHolder(b)
            }
            else -> {
                val b = ItemMessageBotBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                BotViewHolder(b)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserViewHolder -> holder.bind(currentList[position])
            is BotViewHolder -> {
                if (position == currentList.size && streamingResponse != null) {
                    holder.bindWithText(streamingResponse!!)
                } else {
                    holder.bind(currentList[position])
                }
            }
        }
    }

    fun setStreamingResponse(text: String) {
        streamingResponse = text
        notifyItemChanged(currentList.size)
    }

    class UserViewHolder(private val b: ItemMessageUserBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MainViewModel.ChatMessage) {
            b.tvUserMsg.text = msg.content
        }
    }

    class BotViewHolder(private val b: ItemMessageBotBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MainViewModel.ChatMessage) {
            b.tvBotMsg.text = msg.content
        }
        fun bindWithText(text: String) {
            b.tvBotMsg.text = text
        }
    }

    class Diff : DiffUtil.ItemCallback<MainViewModel.ChatMessage>() {
        override fun areItemsTheSame(
            oldItem: MainViewModel.ChatMessage,
            newItem: MainViewModel.ChatMessage
        ) = oldItem === newItem

        override fun areContentsTheSame(
            oldItem: MainViewModel.ChatMessage,
            newItem: MainViewModel.ChatMessage
        ) = oldItem == newItem
    }
}
