/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import android.util.Base64
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import uk.nhs.nhsx.sonar.android.client.CoLocationData
import uk.nhs.nhsx.sonar.android.client.CoLocationEvent
import javax.inject.Inject
import javax.inject.Named

class CoLocationDataProvider @Inject constructor(
    private val contactEventDao: ContactEventDao,
    private val sonarIdProvider: SonarIdProvider,
    @Named(BluetoothModule.ENCRYPT_SONAR_ID)
    private val encryptSonarId: Boolean
) {

    suspend fun getData(): CoLocationData {
        val events = contactEventDao.getAll().map(::convert)
        return CoLocationData(sonarIdProvider.getSonarId(), events)
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
