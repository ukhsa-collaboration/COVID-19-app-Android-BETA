package com.example.colocate.ble

import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class LongLiveConnectionScanTest {

    private val bleClient = mockk<RxBleClient>()
    private val scanResult = mockk<ScanResult>()
    private val bleDevice = mockk<RxBleDevice>()
    private val connection = mockk<RxBleConnection>()
    private val saveContactWorker = mockk<SaveContactWorker>(relaxed = true)

    private val identifier = Identifier(UUID.randomUUID())
    private val timestamp = Date()
    private val rssiValues = listOf(-50, -49)
    private val duration = 5L
    private val period = 50L

    @Before
    fun setUp() {
        every {
            bleClient.scanBleDevices(
                any<ScanSettings>(),
                any(),
                any()
            )
        } returns Observable.just(
            scanResult
        )
        every { scanResult.bleDevice } returns bleDevice
        every { bleDevice.connectionState } returns RxBleConnection.RxBleConnectionState.DISCONNECTED
        every { bleDevice.macAddress } returns "00:1B:44:11:3A:B7"

        every { bleDevice.establishConnection(false) } returns Observable.merge(
            Observable.just(connection),
            Observable
                .timer(period + 1, TimeUnit.MILLISECONDS)
                .flatMap { Observable.error<RxBleConnection>(RuntimeException()) }
        )
        every { connection.readRssi() } returnsMany rssiValues.map { Single.just(it) }
        every { connection.readCharacteristic(DEVICE_CHARACTERISTIC_UUID) } returns Single.just(
            identifier.asBytes
        )
    }

    @Test
    fun connectionWithSingularDevice() {
        runBlocking {
            val sut = LongLiveConnectionScan(
                bleClient, saveContactWorker,
                startTimestampProvider = { timestamp },
                endTimestampProvider = { Date(timestamp.time + duration * 1_000) },
                periodInMilliseconds = period
            )
            sut.start(this)

            val scope = this
            delay(2 * period)

            val recordSlot = slot<SaveContactWorker.Record>()
            verify { saveContactWorker.saveContactEventV2(scope, capture(recordSlot)) }

            val record = recordSlot.captured
            assertThat(record.sonarId).isEqualTo(identifier)
            assertThat(record.duration).isEqualTo(duration)
            assertThat(record.timestamp).isEqualTo(timestamp)
            assertThat(record.rssiValues).containsAll(rssiValues)
        }
    }
}
