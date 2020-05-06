/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEvent
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import kotlin.random.Random

@ExperimentalCoroutinesApi
class SaveContactWorkerTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val contactEventDao = mockk<ContactEventDao>()
    private val saveContactWorker = SaveContactWorker(testDispatcher, contactEventDao)

    @Test
    fun `does not save events that contain too short bluetooth identifier`() {
        runBlocking {
            saveContactWorker.createOrUpdateContactEvent(this, ByteArray(63), -42, DateTime.now(), 47)
            coVerify(exactly = 0) { contactEventDao.createOrUpdate(any()) }
        }
    }

    @Test
    fun `does not save events that contain too long bluetooth identifier`() {
        runBlocking {
            saveContactWorker.createOrUpdateContactEvent(this, ByteArray(150), -42, DateTime.now(), 47)
            coVerify(exactly = 0) { contactEventDao.createOrUpdate(any()) }
        }
    }

    @Test
    fun `saves correct events`() {
        runBlocking {
            val countryCode = byteArrayOf('G'.toByte(), 'B'.toByte())
            val cryptogram = Cryptogram.fromBytes(Random.nextBytes(Cryptogram.SIZE))
            val hmacSignature = Random.nextBytes(16)
            val bluetoothIdentifier = BluetoothIdentifier(
                countryCode,
                cryptogram,
                -7,
                45,
                hmacSignature
            )
            val timestamp = DateTime.now()
            saveContactWorker.createOrUpdateContactEvent(
                this,
                bluetoothIdentifier.asBytes(),
                -42,
                timestamp,
                -8
            )

            val expectedEvent = ContactEvent(
                sonarId = bluetoothIdentifier.cryptogram.asBytes(),
                rssiValues = listOf(-42),
                rssiTimestamps = listOf(timestamp.millis),
                txPowerInProtocol = -7,
                duration = 60,
                timestamp = timestamp.millis,
                txPowerAdvertised = -8,
                transmissionTime = 45,
                hmacSignature = hmacSignature,
                countryCode = countryCode
            )
            coVerify { contactEventDao.createOrUpdate(expectedEvent) }
        }
    }
}
