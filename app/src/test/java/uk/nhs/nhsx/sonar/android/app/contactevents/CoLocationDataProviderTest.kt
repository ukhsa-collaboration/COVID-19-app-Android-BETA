/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import android.util.Base64
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CoLocationDataProviderTest {
    private val contactEventDao = mockk<ContactEventDao>()

    private val events = listOf(
        ContactEvent(
            sonarId = byteArrayOf(),
            rssiValues = listOf(-49, -48, -50),
            rssiTimestamps = listOf(1_000, 4_000, 70_000),
            timestamp = 0,
            duration = 70
        )
    )

    @Test
    fun `returns rssi offsets as seconds since the last rssi reading`() {
        mockkStatic(Base64::class)
        val arraySlot = slot<ByteArray>()
        every {
            Base64.encodeToString(capture(arraySlot), Base64.DEFAULT)
        } answers {
            java.util.Base64.getEncoder().encodeToString(arraySlot.captured)
        }

        coEvery { contactEventDao.getAll() } returns events
        val provider = CoLocationDataProvider(contactEventDao)
        runBlocking {
            val event = provider.getEvents().first()
            assertThat(event.rssiOffsets).isEqualTo(listOf(0, 3, 66))
        }
    }
}
