package com.example.samplebluetooth

import java.nio.ByteBuffer
import java.util.*


 const val APP_UUID = "c1f5983c-fa94-4ac8-8e2e-bb86d6de9b21"

fun getIdAsByte(uuid: UUID): ByteArray {
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(uuid.mostSignificantBits)
    bb.putLong(uuid.leastSignificantBits)
    return bb.array()
}
