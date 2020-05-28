/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Seconds
import timber.log.Timber
import java.lang.Integer.min

@Dao
interface ContactEventDao {
    @Insert
    fun insert(contactEvent: ContactEvent)

    @Update
    fun update(contactEvent: ContactEvent)

    @Transaction
    suspend fun createOrUpdate(newEvent: ContactEvent) {
        val storedEvent = get(newEvent.sonarId)
        if (storedEvent == null) {
            Timber.d("saving No matching event; inserting $newEvent")
            insert(newEvent)
            return
        }
        val mergedEvent = merge(newEvent, storedEvent)
        Timber.d("saving Updated event")
        update(mergedEvent)
    }

    @Query("SELECT * FROM ${ContactEvent.TABLE_NAME} WHERE sonarId=:sonarId")
    suspend fun get(sonarId: ByteArray): ContactEvent?

    @Query("SELECT * FROM ${ContactEvent.TABLE_NAME}")
    suspend fun getAll(): List<ContactEvent>

    @Query("DELETE FROM ${ContactEvent.TABLE_NAME}")
    suspend fun clearEvents()

    @Query("DELETE FROM ${ContactEvent.TABLE_NAME} WHERE timestamp < :timestamp")
    suspend fun clearOldEvents(timestamp: Long)

    @Query("SELECT count(1) FROM ${ContactEvent.TABLE_NAME}")
    suspend fun countEvents(): Long

    @Query("SELECT count(1) FROM ${ContactEvent.TABLE_NAME} WHERE :from <= timestamp AND timestamp <= :to")
    suspend fun countEvents(from: Long, to: Long): Long
}

fun merge(
    newEvent: ContactEvent,
    storedEvent: ContactEvent
): ContactEvent {
    val newEventTime = DateTime(newEvent.timestamp, DateTimeZone.UTC)
    val storedEventTimeStart = DateTime(storedEvent.timestamp, DateTimeZone.UTC)

    val rssis = storedEvent.rssiValues
    val rssiTimestamps = storedEvent.rssiTimestamps

    val mergedRssisAndTimestamps =
        (rssis.zip(rssiTimestamps) +
            newEvent.rssiValues.zip(newEvent.rssiTimestamps))
            .sortedBy { it.second }
    val updatedStartTime = earliest(storedEventTimeStart, newEventTime)
    val updatedTxPowerInProtocol = if (updatedStartTime == storedEventTimeStart) {
        storedEvent.txPowerInProtocol
    } else {
        newEvent.txPowerInProtocol
    }
    val updatedTxPowerAdvertised = if (updatedStartTime == storedEventTimeStart) {
        storedEvent.txPowerAdvertised
    } else {
        newEvent.txPowerAdvertised
    }

    val lastTimestamp = DateTime(mergedRssisAndTimestamps.last().second)
    return storedEvent.copy(
        timestamp = updatedStartTime.millis,
        rssiValues = mergedRssisAndTimestamps.map { it.first },
        rssiTimestamps = mergedRssisAndTimestamps.map { it.second },
        duration = Seconds.secondsBetween(updatedStartTime, lastTimestamp).seconds,
        transmissionTime = min(storedEvent.transmissionTime, newEvent.transmissionTime),
        txPowerAdvertised = updatedTxPowerAdvertised,
        txPowerInProtocol = updatedTxPowerInProtocol
    )
}

fun earliest(first: DateTime, second: DateTime): DateTime =
    if (first.isBefore(second)) {
        first
    } else {
        second
    }
