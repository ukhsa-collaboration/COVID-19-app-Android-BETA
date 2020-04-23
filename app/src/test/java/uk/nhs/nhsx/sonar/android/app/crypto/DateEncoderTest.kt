/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test

class DateEncoderTest {

    @Test
    fun `correctly encodes time as unsigned int`() {
        val maxIntDate = DateTime(Int.MAX_VALUE.toLong() * 1000)
        val maxIntTimestamp = maxIntDate.encodeAsSecondsSinceEpoch()
        assertThat(maxIntTimestamp).isEqualTo(byteArrayOf(127, -1, -1, -1))

        val unsignedIntMaxDate = DateTime(4294967295L * 1000)
        val unsignedIntMaxTimestamp = unsignedIntMaxDate.encodeAsSecondsSinceEpoch()
        assertThat(unsignedIntMaxTimestamp).isEqualTo(byteArrayOf(-1, -1, -1, -1))
    }

    @Test
    fun `correctly adds offset`() {
        val maxIntDate = DateTime((Int.MAX_VALUE - 5).toLong() * 1000)
        val maxIntTimestamp = maxIntDate.encodeAsSecondsSinceEpoch(5)
        assertThat(maxIntTimestamp).isEqualTo(byteArrayOf(127, -1, -1, -1))
    }
}
