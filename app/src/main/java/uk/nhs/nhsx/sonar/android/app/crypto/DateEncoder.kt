/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.joda.time.DateTime
import java.nio.ByteBuffer

// TODO: Will break after Sun Feb 07 06:28:15 GMT 2106.
// If you're looking at this, we're sorry.
fun DateTime.encodeAsSecondsSinceEpoch(offset: Int = 0): ByteArray {
    val secondsSinceEpoch = this.plusSeconds(offset).millis / 1_000
    val bb = ByteBuffer.wrap(ByteArray(8))
    bb.putLong(secondsSinceEpoch)
    return bb.array().sliceArray((4 until 8))
}
