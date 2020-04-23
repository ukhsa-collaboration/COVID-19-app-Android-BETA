/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.ble.Identifier

class ContactEventDaoTest {
    private val contactEventDao = mockk<ContactEventDao>(relaxed = true)
    private val uuid = Identifier.fromString("04330a56-ad45-4b0f-81ee-dd414910e1f5").asBytes

    @Before
    fun setUp() {
        coEvery { contactEventDao.get(uuid) } returns storedEvent
    }

    private val storedEvent =
        ContactEvent(
            sonarId = uuid,
            rssiValues = listOf(1, 2, 3),
            rssiTimestamps = listOf(2000, 3000, 63_000),
            duration = 61,
            timestamp = 2000
        )

    @Test
    fun `extends an existing event into the past`() {
        runBlocking {
            val pastEvent = ContactEvent(
                sonarId = uuid,
                rssiValues = listOf(4),
                rssiTimestamps = listOf(1000),
                duration = 60,
                timestamp = 1000
            )

            assertThat(merge(pastEvent, storedEvent)).isEqualTo(
                ContactEvent(
                    sonarId = uuid,
                    rssiValues = listOf(4, 1, 2, 3),
                    rssiTimestamps = listOf(1000, 2000, 3000, 63_000),
                    duration = 62,
                    timestamp = 1000
                )
            )
        }
    }

    @Test
    fun `extends an existing into the future`() {
        runBlocking {
            val futureEvent = ContactEvent(
                sonarId = uuid,
                rssiValues = listOf(4),
                rssiTimestamps = listOf(64_000),
                duration = 60,
                timestamp = 64_000
            )

            assertThat(merge(futureEvent, storedEvent)).isEqualTo(
                ContactEvent(
                    sonarId = uuid,
                    rssiValues = listOf(1, 2, 3, 4),
                    rssiTimestamps = listOf(2000, 3000, 63_000, 64_000),
                    duration = 62,
                    timestamp = 2000
                )
            )
        }
    }

    @Test
    fun `adds a reading in the middle of an existing event`() {
        runBlocking {
            val futureEvent = ContactEvent(
                sonarId = uuid,
                rssiValues = listOf(4),
                rssiTimestamps = listOf(4000),
                duration = 60,
                timestamp = 4000
            )

            assertThat(merge(futureEvent, storedEvent)).isEqualTo(
                ContactEvent(
                    sonarId = uuid,
                    rssiValues = listOf(1, 2, 4, 3),
                    rssiTimestamps = listOf(2000, 3000, 4000, 63_000),
                    duration = 61,
                    timestamp = 2000
                )
            )
        }
    }

    @Test
    fun `merges an event with multiple readings correctly`() {
        runBlocking {
            val longerEvent = ContactEvent(
                sonarId = uuid,
                rssiValues = listOf(4, 5, 6),
                rssiTimestamps = listOf(1000, 4000, 64_000),
                duration = 63,
                timestamp = 1000
            )

            assertThat(merge(longerEvent, storedEvent)).isEqualTo(
                ContactEvent(
                    sonarId = uuid,
                    rssiValues = listOf(4, 1, 2, 5, 3, 6),
                    rssiTimestamps = listOf(1000, 2000, 3000, 4000, 63_000, 64_000),
                    duration = 63,
                    timestamp = 1000
                )
            )
        }
    }
}
