/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.debug

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.event_error_view_item.view.error_event
import kotlinx.android.synthetic.main.event_view.view.remote_contact_id
import kotlinx.android.synthetic.main.event_view.view.rssi
import kotlinx.android.synthetic.main.event_view.view.time
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ble.ConnectedDevice

class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val context: Context = view.context

    fun bindTo(event: ConnectedDevice) {
        itemView.remote_contact_id.text = event.id
        itemView.rssi.text = event.rssiValues.joinToString(",", prefix = "[", postfix = "]")
        val startTime = event.timestamp.split("T").last().replace("Z", "")
        itemView.time.text = context.getString(R.string.started, startTime)
    }
}

class EventErrorViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bindTo(event: ConnectedDevice) {
        val errorTextView = itemView.error_event
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
            val view = inflater.inflate(R.layout.event_error_view_item, parent, false)
            EventErrorViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.event_view, parent, false)
            EventViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is EventViewHolder) holder.bindTo(getItem(position))
        else if (holder is EventErrorViewHolder) holder.bindTo(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        // TODO: Seems to be able to return null
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
