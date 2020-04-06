/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.debug

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.colocate.R
import com.example.colocate.ble.ConnectedDevice
import kotlinx.android.synthetic.main.event_view.view.remote_contact_id
import kotlinx.android.synthetic.main.event_view.view.rssi
import kotlinx.android.synthetic.main.event_view.view.time
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bindTo(event: ConnectedDevice) {
        itemView.remote_contact_id.text = event.id
        itemView.rssi.text = event.rssiValues.joinToString(",", prefix = "[", postfix = "]")
        val startTime = event.timestamp.split("T").last().replace("Z", "")
        itemView.time.text = "Started $startTime"
    }
}

class EventErrorViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bindTo(event: ConnectedDevice) {
        val errorTextView = itemView.findViewById<TextView>(R.id.error_event)
        errorTextView.text = when {
            event.isReadFailure -> {
                "Read failure"
            }
            event.isConnectionError -> {
                "Disconnected " + event.id.orEmpty()
            }
            else -> {
                "None"
            }
        }
    }
}

class EventsAdapter :
    ListAdapter<ConnectedDevice, RecyclerView.ViewHolder>(EventItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == ERROR_TYPE) {
            val view = inflater
                .inflate(
                    R.layout.event_error_view_item,
                    parent,
                    false
                )
            EventErrorViewHolder(view)
        } else {
            val view = inflater.inflate(
                R.layout.event_view,
                parent,
                false
            )
            EventViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is EventViewHolder) holder.bindTo(getItem(position))
        else if (holder is EventErrorViewHolder) holder.bindTo(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item.isConnectionError or item.isReadFailure) ERROR_TYPE
        else CONNECTION_TYPE
    }

    companion object {
        private const val ERROR_TYPE = 1
        private const val CONNECTION_TYPE = 2
    }
}

class EventItemDiffCallback : DiffUtil.ItemCallback<ConnectedDevice>() {
    override fun areItemsTheSame(oldItem: ConnectedDevice, newItem: ConnectedDevice) =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: ConnectedDevice, newItem: ConnectedDevice): Boolean {
        return oldItem == newItem
    }
}
