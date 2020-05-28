/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.debug

import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.event_view.view.detailView
import kotlinx.android.synthetic.main.event_view.view.remote_contact_id
import kotlinx.android.synthetic.main.event_view.view.rssi
import kotlinx.android.synthetic.main.event_view.view.timestamp
import kotlinx.android.synthetic.main.event_view.view.txPower
import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ble.ConnectedDevice
import uk.nhs.nhsx.sonar.android.app.contactevents.timestampsToIntervals
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import kotlin.math.abs

class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val context: Context = view.context

    fun bindTo(event: ConnectedDevice) {
        itemView.detailView.visibility = if (event.expanded) View.VISIBLE else View.GONE
        val cryptogramBytes = Base64.decode(event.cryptogram, Base64.DEFAULT)
        val (cryptogramColour, inverseColour) = cryptogramColourAndInverse(cryptogramBytes)
        updateColours(cryptogramColour, inverseColour)

        itemView.remote_contact_id.text = event.cryptogram
        itemView.remote_contact_id.maxLines = if (event.expanded) 4 else 1

        itemView.detailView.timestamp.text = context.getString(R.string.timestamp, event.firstSeen)
        itemView.detailView.txPower.text =
            context.getString(R.string.txpower, event.txPowerAdvertised, event.txPowerProtocol)

        itemView.rssi.text = if (event.expanded) {
            val rssis = event.rssiValues
            val rssiIntervals = event.rssiTimestamps
                .map { it.millis }
                .timestampsToIntervals()
                .map { abs(it) }
            rssis.mapIndexed { index, _ -> "${rssis[index]}        ${event.rssiTimestamps[index].toTime()}        ${rssiIntervals[index]}" }
                .joinToString("\n")
        } else {
            event.rssiValues.joinToString(",", prefix = "[", postfix = "]")
        }
        itemView.rssi.maxLines = if (event.expanded) Int.MAX_VALUE else 1
    }

    private fun DateTime.toTime(): String = this.toUtcIsoFormat().split("T")[1].substring(0, 8)
    private fun updateColours(cryptogramColour: Int, inverseColor: Int) {
        itemView.setBackgroundColor(cryptogramColour)
        itemView.remote_contact_id.setTextColor(inverseColor)
        itemView.rssi.setTextColor(inverseColor)
        for (view in itemView.detailView.children) {
            if (view is TextView) view.setTextColor(inverseColor)
        }
    }
}

class EventsAdapter :
    ListAdapter<ConnectedDevice, RecyclerView.ViewHolder>(EventItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.event_view, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val eventHolder = holder as EventViewHolder
        eventHolder.bindTo(getItem(position))
        val event = currentList[position]
        eventHolder.itemView.setOnClickListener {
            event.expanded = !event.expanded
            notifyItemChanged(position)
        }
    }
}

class EventItemDiffCallback : DiffUtil.ItemCallback<ConnectedDevice>() {
    override fun areItemsTheSame(oldItem: ConnectedDevice, newItem: ConnectedDevice): Boolean =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: ConnectedDevice, newItem: ConnectedDevice): Boolean =
        oldItem == newItem
}
