/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.joda.time.DateTime
import java.nio.ByteBuffer

// Will break after Sun Feb 07 06:28:15 GMT 2106.
// If you're looking at this because of that, we're sorry.
fun DateTime.encodeAsSecondsSinceEpoch(): ByteArray {
    val secondsSinceEpoch = this.millis / 1_000
    val bb = ByteBuffer.wrap(ByteArray(8))
    bb.putLong(secondsSinceEpoch)
    return bb.array().sliceArray((4 until 8))
}
