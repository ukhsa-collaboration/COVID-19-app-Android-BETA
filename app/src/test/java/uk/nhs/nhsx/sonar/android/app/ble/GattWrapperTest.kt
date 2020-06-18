/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt.GATT_FAILURE
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdProvider
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import java.util.UUID
import kotlin.random.Random

@ExperimentalCoroutinesApi
class GattWrapperTest {
    private val server = spyk<BluetoothGattServer>()
    private val bluetoothManager = mockk<BluetoothManager>()
    private val bluetoothIdProvider = mockk<BluetoothIdProvider>()
    private val coroutineScope = TestCoroutineScope()
    private val device = mockk<BluetoothDevice>()
    private val keepAliveCharacteristic = spyk(
        BluetoothGattCharacteristic(
            SONAR_KEEPALIVE_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ + BluetoothGattCharacteristic.PROPERTY_WRITE + BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE + BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ + BluetoothGattCharacteristic.PERMISSION_WRITE
        )
    )

    private lateinit var identifierBytes: ByteArray

    private val fixedRandom = Random(5903)
    private val randomValueGenerator = { fixedRandom.nextBytes(1) }
    private val gattWrapper = GattWrapper(
        server,
        coroutineScope,
        bluetoothManager,
        bluetoothIdProvider,
        keepAliveCharacteristic,
        randomValueGenerator
    )

    @Before
    fun setUp() {
        val cryptogram = Cryptogram.fromBytes(
            Random.Default.nextBytes(Cryptogram.SIZE)
        )
        val bluetoothIdentifier = BluetoothIdentifier(
            "GB".toByteArray(),
            cryptogram,
            (-8).toByte(),
            14,
            Random.nextBytes(16)
        )
        identifierBytes = bluetoothIdentifier.asBytes()
        every { bluetoothIdProvider.provideBluetoothPayload() } returns bluetoothIdentifier
        every { bluetoothIdProvider.canProvideIdentifier() } returns true

        every { device.address } returns "abc-def"
    }

    @After
    fun tearDown() {
        gattWrapper.notifyJob?.cancel()
        coroutineScope.cleanupTestCoroutines()
    }

    @Test
    fun `server notifies subscribers with a random value every eight seconds`() {
        val keepAliveDescriptor = mockk<BluetoothGattDescriptor>()
        val slot = slot<ByteArray>()
        every { keepAliveCharacteristic.setValue(capture(slot)) } answers { true }

        every { keepAliveDescriptor.characteristic.uuid } returns SONAR_KEEPALIVE_CHARACTERISTIC_UUID
        every { keepAliveDescriptor.uuid } returns NOTIFY_DESCRIPTOR_UUID
        every { bluetoothManager.getConnectedDevices(BluetoothProfile.GATT) } returns listOf(device)

        gattWrapper.respondToDescriptorWrite(device, keepAliveDescriptor, false, 75)
        verify(exactly = 0) { server.notifyCharacteristicChanged(any(), any(), any()) }

        coroutineScope.advanceTimeBy(8_000)
        val firstKeepAliveValue = slot.captured
        verify { keepAliveCharacteristic.value = firstKeepAliveValue }
        verify(exactly = 1) {
            server.notifyCharacteristicChanged(
                device,
                keepAliveCharacteristic,
                false
            )
        }

        coroutineScope.advanceTimeBy(8_000)
        val secondKeepAliveValue = slot.captured
        verify { keepAliveCharacteristic.value = secondKeepAliveValue }
        verify(exactly = 2) {
            server.notifyCharacteristicChanged(
                device,
                keepAliveCharacteristic,
                false
            )
        }

        assertThat(secondKeepAliveValue).isNotEqualTo(
            byteArrayOf((firstKeepAliveValue.first() + 1.toByte()).toByte())
        )
    }

    @Test
    fun `first subscriber kicks off notify job`() {
        val keepAliveDescriptor = mockk<BluetoothGattDescriptor>()
        every { keepAliveDescriptor.characteristic.uuid } returns SONAR_KEEPALIVE_CHARACTERISTIC_UUID
        every { keepAliveDescriptor.uuid } returns NOTIFY_DESCRIPTOR_UUID

        gattWrapper.respondToDescriptorWrite(device, keepAliveDescriptor, false, 75)
        assertThat(gattWrapper.notifyJob).isNotNull()
        assertThat(gattWrapper.notifyJob?.isActive).isTrue()
    }

    @Test
    fun `when the last subscriber disconnects the notify job is stopped`() {
        val keepAliveDescriptor = mockk<BluetoothGattDescriptor>()
        every { keepAliveDescriptor.characteristic.uuid } returns SONAR_KEEPALIVE_CHARACTERISTIC_UUID
        every { keepAliveDescriptor.uuid } returns NOTIFY_DESCRIPTOR_UUID
        val secondDevice = mockk<BluetoothDevice>()

        gattWrapper.respondToDescriptorWrite(device, keepAliveDescriptor, false, 75)
        gattWrapper.respondToDescriptorWrite(secondDevice, keepAliveDescriptor, false, 75)

        assertThat(gattWrapper.notifyJob).isNotNull()
        assertThat(gattWrapper.notifyJob?.isActive).isTrue()

        gattWrapper.deviceDisconnected(device)

        assertThat(gattWrapper.notifyJob).isNotNull()
        assertThat(gattWrapper.notifyJob?.isActive).isTrue()

        gattWrapper.deviceDisconnected(secondDevice)

        assertThat(gattWrapper.notifyJob?.isActive).isFalse()
    }

    @Test
    fun `allows notification to be set up for identity characteristic but does not send out values yet`() {
        val notifyDescriptor = mockk<BluetoothGattDescriptor>()
        every { notifyDescriptor.characteristic.uuid } returns SONAR_IDENTITY_CHARACTERISTIC_UUID
        every { notifyDescriptor.uuid } returns NOTIFY_DESCRIPTOR_UUID

        gattWrapper.respondToDescriptorWrite(device, notifyDescriptor, true, 75)

        verify(exactly = 0) { server.sendResponse(device, 75, GATT_FAILURE, 0, byteArrayOf()) }
        coroutineScope.advanceTimeBy(8_000)
        verify(exactly = 0) { server.notifyCharacteristicChanged(any(), any(), any()) }
    }

    @Test
    fun `rejects descriptor writes for unknown identifiers`() {
        val unknownDescriptor = mockk<BluetoothGattDescriptor>()
        every { unknownDescriptor.characteristic.uuid } returns UUID.randomUUID()
        every { unknownDescriptor.uuid } returns UUID.randomUUID()

        gattWrapper.respondToDescriptorWrite(device, unknownDescriptor, true, 75)

        every { unknownDescriptor.uuid } returns NOTIFY_DESCRIPTOR_UUID
        gattWrapper.respondToDescriptorWrite(device, unknownDescriptor, true, 75)

        verify(exactly = 2) { server.sendResponse(device, 75, GATT_FAILURE, 0, byteArrayOf()) }
    }

    @Test
    fun `characteristic reads for identity characteristic return bluetooth identifier`() {
        val identityCharacteristic = mockk<BluetoothGattCharacteristic>()
        every { identityCharacteristic.uuid } returns SONAR_IDENTITY_CHARACTERISTIC_UUID

        gattWrapper.respondToCharacteristicRead(device, 45, identityCharacteristic)
        verify { server.sendResponse(device, 45, GATT_SUCCESS, 0, identifierBytes) }
    }

    @Test
    fun `characteristic reads for keep alive return are successful`() {
        val keepAlive = mockk<BluetoothGattCharacteristic>()
        every { keepAlive.uuid } returns SONAR_KEEPALIVE_CHARACTERISTIC_UUID

        gattWrapper.respondToCharacteristicRead(device, 45, keepAlive)
        verify { server.sendResponse(device, 45, GATT_SUCCESS, 0, byteArrayOf()) }
    }

    @Test
    fun `characteristic reads for unknown uuids responds with failure`() {
        val unknownCharacteristics = mockk<BluetoothGattCharacteristic>()
        every { unknownCharacteristics.uuid } returns UUID.randomUUID()

        gattWrapper.respondToCharacteristicRead(device, 45, unknownCharacteristics)
        verify { server.sendResponse(device, 45, GATT_FAILURE, 0, byteArrayOf()) }
    }
}
