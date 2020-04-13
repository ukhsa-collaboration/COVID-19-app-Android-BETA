/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import uk.nhs.nhsx.sonar.android.client.colocation.Seconds
import java.nio.ByteBuffer
import java.util.Date

// TODO: Will break after Sun Feb 07 06:28:15 GMT 2106.
// If you're looking at this, we're sorry.
fun Date.encodeAsSecondsSinceEpoch(offset: Seconds = 0): ByteArray {
    val seconds = (this.time / 1_000) + offset
    val bb = ByteBuffer.wrap(ByteArray(8))
    bb.putLong(seconds)
    return bb.array().sliceArray((4 until 8))
}
