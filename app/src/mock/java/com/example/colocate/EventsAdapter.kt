/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.colocate.persistence.ContactEventV2
import kotlinx.android.synthetic.main.event_view.view.remote_contact_id
import kotlinx.android.synthetic.main.event_view.view.rssi

class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bindTo(event: ContactEventV2) {
        itemView.remote_contact_id.text = event.sonarId
        itemView.rssi.text = event.rssiValues.joinToString(",", prefix = "[", postfix = "]")
    }
}

class EventsAdapter : ListAdapter<ContactEventV2, EventViewHolder>(EventItemDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        return EventViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.event_view, parent, false)
        )
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }
}

class EventItemDiffCallback : DiffUtil.ItemCallback<ContactEventV2>() {
    override fun areItemsTheSame(oldItem: ContactEventV2, newItem: ContactEventV2) =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: ContactEventV2, newItem: ContactEventV2): Boolean {
        return oldItem == newItem
    }
}
