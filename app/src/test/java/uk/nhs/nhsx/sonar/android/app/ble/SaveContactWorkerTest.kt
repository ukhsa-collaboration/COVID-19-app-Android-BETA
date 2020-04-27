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

@ExperimentalCoroutinesApi
class SaveContactWorkerTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val contactEventDao = mockk<ContactEventDao>()
    private val saveContactWorker = SaveContactWorker(testDispatcher, contactEventDao)

    @Test
    fun `does not save events that contain too short bluetooth identifier`() {
        runBlocking {
            saveContactWorker.createOrUpdateContactEvent(this, ByteArray(63), -42, DateTime.now())
            coVerify(exactly = 0) { contactEventDao.createOrUpdate(any()) }
        }
    }

    @Test
    fun `does not save events that contain too long bluetooth identifier`() {
        runBlocking {
            saveContactWorker.createOrUpdateContactEvent(this, ByteArray(150), -42, DateTime.now())
            coVerify(exactly = 0) { contactEventDao.createOrUpdate(any()) }
        }
    }

    @Test
    fun `saves correct events`() {
        runBlocking {
            val countryCode = byteArrayOf('G'.toByte(), 'B'.toByte())
            val cryptogram = Cryptogram(ByteArray(64), ByteArray(26), ByteArray(16))
            val bluetoothIdentifier = BluetoothIdentifier(countryCode, cryptogram, -7)
            val timestamp = DateTime.now()
            saveContactWorker.createOrUpdateContactEvent(
                this,
                bluetoothIdentifier.asBytes(),
                -42,
                timestamp
            )

            val expectedEvent = ContactEvent(
                sonarId = bluetoothIdentifier.asBytes(),
                rssiValues = listOf(-42),
                rssiTimestamps = listOf(timestamp.millis),
                txPower = -7,
                duration = 60,
                timestamp = timestamp.millis
            )
            coVerify { contactEventDao.createOrUpdate(expectedEvent) }
        }
    }

    // TODO: Remove once iOS is sending txPower
    @Test
    fun `saves events if they are just missing txPower`() {
        runBlocking {
            val countryCode = byteArrayOf('G'.toByte(), 'B'.toByte())
            val cryptogram = Cryptogram(ByteArray(64), ByteArray(26), ByteArray(16))
            val bluetoothIdentifier = BluetoothIdentifier(countryCode, cryptogram, -7)
            val timestamp = DateTime.now()
            saveContactWorker.createOrUpdateContactEvent(
                this,
                bluetoothIdentifier.asBytes().dropLast(1).toByteArray(),
                -42,
                timestamp
            )

            val idWithDefaultTx = bluetoothIdentifier.asBytes()
            idWithDefaultTx[idWithDefaultTx.size - 1] = 0.toByte()
            val expectedEvent = ContactEvent(
                sonarId = idWithDefaultTx,
                rssiValues = listOf(-42),
                rssiTimestamps = listOf(timestamp.millis),
                txPower = 0,
                duration = 60,
                timestamp = timestamp.millis
            )
            coVerify { contactEventDao.createOrUpdate(expectedEvent) }
        }
    }
}
