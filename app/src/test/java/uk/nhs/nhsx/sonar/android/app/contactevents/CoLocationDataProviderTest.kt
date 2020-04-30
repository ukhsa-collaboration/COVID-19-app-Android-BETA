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
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationEvent
import java.util.Base64
import kotlin.random.Random

class CoLocationDataProviderTest {

    private val contactEventDao = mockk<ContactEventDao>()
    private val base64encode = Base64.getEncoder()::encodeToString
    private val provider = CoLocationDataProvider(contactEventDao, base64encode)

    @Test
    fun testGetEvents() {
        val cryptogram = Cryptogram.fromBytes(Random.nextBytes(106))
        val signature = Random.nextBytes(16)
        val contactEvent = ContactEvent(
            sonarId = cryptogram.asBytes(),
            rssiValues = listOf(-49, -48, -50),
            rssiTimestamps = listOf(1_000, 4_000, 70_000),
            timestamp = DateTime.parse("2020-04-24T12:30:00Z").millis,
            duration = 70,
            txPowerInProtocol = (-8).toByte(),
            txPowerAdvertised = (-4).toByte(),
            countryCode = "GB".toByteArray(),
            hmacSignature = signature,
            transmissionTime = 4
        )
        coEvery { contactEventDao.getAll() } returns listOf(contactEvent)

        runBlocking {
            val coLocationEvent = provider.getEvents().first()

            val rssiValues = listOf(-49, -48, -50).map { it.toByte() }.toByteArray()
            assertThat(coLocationEvent).isEqualTo(
                CoLocationEvent(
                    encryptedRemoteContactId = base64encode(cryptogram.asBytes()),
                    rssiValues = base64encode(rssiValues),
                    rssiIntervals = listOf(0, 3, 66),
                    timestamp = "2020-04-24T12:30:00Z",
                    duration = 70,
                    txPowerInProtocol = (-8).toByte(),
                    txPowerAdvertised = (-4).toByte(),
                    hmacSignature = base64encode(signature),
                    countryCode = 18242.toShort(),
                    transmissionTime = 4
                )
            )
        }
    }
}
