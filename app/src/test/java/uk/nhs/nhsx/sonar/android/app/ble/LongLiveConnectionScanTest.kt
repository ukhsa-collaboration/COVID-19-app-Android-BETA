/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.util.Base64
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import net.lachlanmckee.timberjunit.TimberTestRule
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.util.concurrent.TimeUnit

class LongLiveConnectionScanTest {

    private val bleClient = mockk<RxBleClient>()
    private val scanResult = mockk<ScanResult>()
    private val bleDevice = mockk<RxBleDevice>()
    private val connection = mockk<RxBleConnection>()
    private val saveContactWorker = FakeSaveContactWorker()

    private val timestamp = DateTime.now(DateTimeZone.UTC)
    private val rssiValues = listOf(-50, -49)
    private val duration = 5
    private val period = 50L
    private lateinit var identifier: Identifier

    @Rule
    @JvmField
    val logAllOnFailuresRule: TimberTestRule = TimberTestRule.logAllWhenTestFails()

    @Before
    fun setUp() {
        Timber.plant(Timber.DebugTree())
        every { bleClient.observeStateChanges() } returns Observable.empty()
        every { bleClient.state } returns RxBleClient.State.READY
        every {
            bleClient.scanBleDevices(
                any<ScanSettings>(),
                any(),
                any()
            )
        } returns Observable.just(scanResult)

        every { scanResult.bleDevice } returns bleDevice
        every { bleDevice.connectionState } returns RxBleConnection.RxBleConnectionState.DISCONNECTED
        every { bleDevice.macAddress } returns "00:1B:44:11:3A:B7"

        every { bleDevice.establishConnection(false) } returns Observable.merge(
            Observable.just(connection),
            Observable
                .timer(period + 25, TimeUnit.MILLISECONDS)
                .flatMap {
                    val disconnectException = BleDisconnectedException.adapterDisabled("")
                    Observable.error<RxBleConnection>(disconnectException)
                }
        )
        every { connection.readRssi() } returnsMany rssiValues.map { Single.just(it) }

        `bypass android_util_Base64 to java_util_Base64`()
        identifier = Identifier.fromBytes(ByteArray(64) { 1 })
        every { connection.readCharacteristic(DEVICE_CHARACTERISTIC_UUID) } returns Single.just(
            identifier.asBytes
        )
    }

    @Test
    fun `bypass android_util_Base64 to java_util_Base64`() {
        mockkStatic(Base64::class)
        val arraySlot = slot<ByteArray>()

        every {
            Base64.encodeToString(capture(arraySlot), Base64.DEFAULT)
        } answers {
            java.util.Base64.getEncoder().encodeToString(arraySlot.captured)
        }

        val stringSlot = slot<String>()
        every {
            Base64.decode(capture(stringSlot), Base64.DEFAULT)
        } answers {
            java.util.Base64.getDecoder().decode(stringSlot.captured)
        }
    }

    @Test
    fun connectionWithSingularDevice() {
        runBlocking {
            val sut = LongLiveConnectionScan(
                bleClient,
                saveContactWorker,
                startTimestampProvider = { timestamp },
                endTimestampProvider = { timestamp.plusSeconds(duration) },
                periodInMilliseconds = period,
                bleEvents = BleEvents()
            )
            sut.start(this)

            await untilNotNull { saveContactWorker.savedRecord }

            val record = saveContactWorker.savedRecord!!
            assertThat(record.sonarId.asBytes).isEqualTo(identifier.asBytes)
            assertThat(record.duration).isEqualTo(duration.toLong())
            assertThat(record.timestamp).isEqualTo(timestamp)
            assertThat(record.rssiValues).containsAll(rssiValues)

            assertThat(saveContactWorker.saveScope).isEqualTo(this)
        }
    }
}

private class FakeSaveContactWorker : SaveContactWorker by mockk() {
    var saveScope: CoroutineScope? = null
        private set
    var savedRecord: SaveContactWorker.Record? = null
        private set

    override fun saveContactEvent(scope: CoroutineScope, record: SaveContactWorker.Record) {
        saveScope = scope
        savedRecord = record
    }
}
