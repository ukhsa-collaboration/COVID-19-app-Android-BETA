/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.Before
import org.junit.Test

class DateEncoderTest {
    @Before
    fun setUp() {
        DateTimeZone.setProvider(UTCProvider())
    }

    @Test
    fun `correctly encodes time as unsigned int`() {
        val maxIntDate = DateTime(Int.MAX_VALUE.toLong() * 1000)
        assertThat(maxIntDate.encodeAsSecondsSinceEpoch())
            .isEqualTo(byteArrayOf(127, -1, -1, -1))

        val unsignedIntMaxDate = DateTime(4294967295L * 1000)
        assertThat(unsignedIntMaxDate.encodeAsSecondsSinceEpoch())
            .isEqualTo(byteArrayOf(-1, -1, -1, -1))
    }

    @Test
    fun `correctly adds offset`() {
        val maxIntDate = DateTime((Int.MAX_VALUE - 5).toLong() * 1000)
        assertThat(maxIntDate.encodeAsSecondsSinceEpoch(5))
            .isEqualTo(byteArrayOf(127, -1, -1, -1))
    }
}
