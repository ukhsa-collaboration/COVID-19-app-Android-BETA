/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import android.util.Base64
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import uk.nhs.nhsx.sonar.android.client.CoLocationEvent
import javax.inject.Inject
import javax.inject.Named

class CoLocationDataProvider @Inject constructor(
    private val contactEventDao: ContactEventDao,
    @Named(BluetoothModule.ENCRYPT_SONAR_ID)
    private val encryptSonarId: Boolean
) {

    suspend fun getEvents(): List<CoLocationEvent> {
        return contactEventDao.getAll().map(::convert)
    }

    private fun convert(contactEvent: ContactEvent): CoLocationEvent {
        val event = CoLocationEvent(
            rssiValues = contactEvent.rssiValues,
            timestamp = DateTime(contactEvent.timestamp, DateTimeZone.UTC).toUtcIsoFormat(),
            duration = contactEvent.duration
        )

        return if (encryptSonarId) {
            val encryptedRemoteContactId = Base64.encodeToString(
                contactEvent.sonarId,
                Base64.DEFAULT
            )
            event.copy(
                encryptedRemoteContactId = encryptedRemoteContactId
            )
        } else {
            event.copy(sonarId = contactEvent.idAsString())
        }
    }

    suspend fun clearData() {
        contactEventDao.clearEvents()
    }
}
