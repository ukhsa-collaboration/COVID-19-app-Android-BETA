/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import java.nio.ByteBuffer
import java.util.UUID

data class Identifier(private val uuid: UUID) {

    val asBytes: ByteArray by lazy {
        ByteBuffer.wrap(ByteArray(16)).also {
            it.putLong(uuid.mostSignificantBits)
            it.putLong(uuid.leastSignificantBits)
        }.array()
    }

    val asString: String = uuid.toString()

    override fun toString() = "Identifier[uuid: $uuid]"

    companion object {
        fun fromString(uuid: String): Identifier =
            Identifier(UUID.fromString(uuid))

        fun fromBytes(bytes: ByteArray): Identifier =
            ByteBuffer.wrap(bytes).let {
                Identifier(UUID(it.long, it.long))
            }
    }
}
