package uk.nhs.nhsx.sonar.android.app.contactevents

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import uk.nhs.nhsx.sonar.android.client.CoLocationData
import uk.nhs.nhsx.sonar.android.client.CoLocationEvent
import javax.inject.Inject

class CoLocationDataProvider @Inject constructor(
    private val contactEventDao: ContactEventDao,
    private val sonarIdProvider: SonarIdProvider
) {

    suspend fun getData(): CoLocationData {
        val events = contactEventDao.getAll().map(::convert)
        return CoLocationData(sonarIdProvider.getSonarId(), events)
    }

    private fun convert(contactEvent: ContactEvent): CoLocationEvent =
        CoLocationEvent(
            sonarId = contactEvent.idAsString(),
            rssiValues = contactEvent.rssiValues,
            timestamp = DateTime(contactEvent.timestamp, DateTimeZone.UTC).toUtcIsoFormat(),
            duration = contactEvent.duration
        )

    suspend fun clearData() {
        contactEventDao.clearEvents()
    }
}
