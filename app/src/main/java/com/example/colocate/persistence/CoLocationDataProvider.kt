package com.example.colocate.persistence

import com.example.colocate.di.module.BluetoothModule.Companion.USE_CONNECTION_V2
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationData
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationEvent
import javax.inject.Inject
import javax.inject.Named

class CoLocationDataProvider @Inject constructor(
    @Named(USE_CONNECTION_V2) private val useConnectionV2: Boolean,
    private val contactEventDao: ContactEventDao,
    private val contactEventV2Dao: ContactEventV2Dao,
    private val residentIdProvider: ResidentIdProvider
) {

    suspend fun getData(): CoLocationData =
        if (useConnectionV2) {
            val events = contactEventV2Dao.getAll().map(::convert)
            CoLocationData(residentIdProvider.getResidentId(), events)
        } else {
            val events = contactEventDao.getAll().map(::convert)
            CoLocationData(residentIdProvider.getResidentId(), events)
        }

    private fun convert(contactEvent: ContactEvent): CoLocationEvent =
        CoLocationEvent(
            sonarId = contactEvent.remoteContactId,
            rssiValues = listOf(contactEvent.rssi),
            timestamp = contactEvent.timestamp,
            duration = -1
        )

    private fun convert(contactEvent: ContactEventV2): CoLocationEvent =
        CoLocationEvent(
            sonarId = contactEvent.sonarId,
            rssiValues = contactEvent.rssiValues,
            timestamp = contactEvent.timestamp,
            duration = contactEvent.duration
        )
}
