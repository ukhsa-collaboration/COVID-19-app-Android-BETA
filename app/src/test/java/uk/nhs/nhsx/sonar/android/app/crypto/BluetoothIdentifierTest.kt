/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.random.Random

class BluetoothIdentifierTest {
    @Test
    fun `can convert to bytes and back`() {
        val countryCode = byteArrayOf('G'.toByte(), 'B'.toByte())
        val cryptogram = Cryptogram.fromBytes(Random.nextBytes(Cryptogram.SIZE))
        val signature = Random.nextBytes(16)
        val bluetoothIdentifier = BluetoothIdentifier(
            countryCode,
            cryptogram,
            -7,
            45,
            signature
        )

        assertThat(bluetoothIdentifier.asBytes()).hasSize(BluetoothIdentifier.SIZE)
        assertThat(bluetoothIdentifier).isEqualTo(BluetoothIdentifier.fromBytes(bluetoothIdentifier.asBytes()))
    }

    @Test
    fun `can convert from bytes and back`() {
        val countryCode = byteArrayOf('G'.toByte(), 'B'.toByte())
        val cryptogram = Cryptogram.fromBytes(Random.nextBytes(Cryptogram.SIZE))
        val txPower = 124.toByte()
        val transmissionTimeBytes = ByteArray(4) { 0.toByte() }
        val signature = Random.nextBytes(16)
        val idBytes = countryCode + cryptogram.asBytes() + txPower + transmissionTimeBytes + signature
        val bluetoothId = BluetoothIdentifier.fromBytes(
            idBytes
        )
        assertThat(bluetoothId.countryCode).isEqualTo(countryCode)
        assertThat(bluetoothId.cryptogram).isEqualTo(cryptogram)
        assertThat(bluetoothId.txPower).isEqualTo(txPower)
        assertThat(bluetoothId.transmissionTime).isEqualTo(ByteBuffer.wrap(transmissionTimeBytes).int)
        assertThat(bluetoothId.hmacSignature).isEqualTo(signature)
        assertThat(bluetoothId.asBytes()).isEqualTo(idBytes)
    }
}
