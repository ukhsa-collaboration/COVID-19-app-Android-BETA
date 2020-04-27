/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationEvent
import java.util.Base64

class CoLocationDataProviderTest {

    private val contactEventDao = mockk<ContactEventDao>()
    private val base64encode = Base64.getEncoder()::encodeToString
    private val provider = CoLocationDataProvider(contactEventDao, base64encode)

    @Test
    fun testGetEvents() {
        val contactEvent = ContactEvent(
            sonarId = byteArrayOf('A'.toByte(), 'B'.toByte()),
            rssiValues = listOf(-49, -48, -50),
            rssiTimestamps = listOf(1_000, 4_000, 70_000),
            timestamp = DateTime.parse("2020-04-24T12:30:00Z").millis,
            duration = 70,
            txPower = (-8).toByte()
        )
        coEvery { contactEventDao.getAll() } returns listOf(contactEvent)

        runBlocking {
            val coLocationEvent = provider.getEvents().first()

            assertThat(coLocationEvent).isEqualTo(
                CoLocationEvent(
                    encryptedRemoteContactId = base64encode(contactEvent.sonarId),
                    rssiValues = listOf(-49, -48, -50),
                    rssiOffsets = listOf(0, 3, 66),
                    timestamp = "2020-04-24T12:30:00Z",
                    duration = 70,
                    txPower = (-8).toByte()
                )
            )
        }
    }
}
