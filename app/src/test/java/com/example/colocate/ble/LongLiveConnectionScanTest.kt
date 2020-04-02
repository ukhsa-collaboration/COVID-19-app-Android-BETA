package com.example.colocate.ble

import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
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
    private val saveContactWorker = FakeSaveContactWorker()

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
                bleClient,
                saveContactWorker,
                startTimestampProvider = { timestamp },
                endTimestampProvider = { Date(timestamp.time + duration * 1_000) },
                periodInMilliseconds = period
            )
            sut.start(this)

            waitUntil {
                saveContactWorker.saveScope == this &&
                    saveContactWorker.savedRecord != null
            }

            val record = saveContactWorker.savedRecord!!
            assertThat(record.sonarId).isEqualTo(identifier)
            assertThat(record.duration).isEqualTo(duration)
            assertThat(record.timestamp).isEqualTo(timestamp)
            assertThat(record.rssiValues).containsAll(rssiValues)
        }
    }
}

private class FakeSaveContactWorker : SaveContactWorker by mockk() {
    var saveScope: CoroutineScope? = null
        private set
    var savedRecord: SaveContactWorker.Record? = null
        private set

    override fun saveContactEventV2(scope: CoroutineScope, record: SaveContactWorker.Record) {
        saveScope = scope
        savedRecord = record
    }
}

private fun waitUntil(predicate: () -> Boolean) {
    val maxAttempts = 20
    var attempts = 1

    while (!predicate() && attempts <= maxAttempts) {
        Thread.sleep(20)
        attempts++
    }

    if (!predicate()) {
        fail<String>("Failed waiting for predicate")
    }
}
