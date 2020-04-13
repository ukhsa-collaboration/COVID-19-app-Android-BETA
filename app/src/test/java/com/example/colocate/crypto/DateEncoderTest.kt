/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.crypto

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

class DateEncoderTest {
    @Test
    fun `correctly encodes time as unsigned int`() {
        // multiplied because Java deals with milliseconds
        val maxIntDate = Date(Int.MAX_VALUE.toLong() * 1000)
        assertThat(maxIntDate.encodeAsSecondsSinceEpoch())
            .isEqualTo(byteArrayOf(127, -1, -1, -1))

        val unsignedIntMaxDate = Date(4294967295L * 1000)
        assertThat(unsignedIntMaxDate.encodeAsSecondsSinceEpoch())
            .isEqualTo(byteArrayOf(-1, -1, -1, -1))
    }

    @Test
    fun `correctly adds offset`() {
        // multiplied because Java deals with milliseconds
        val maxIntDate = Date((Int.MAX_VALUE - 5).toLong() * 1000)
        assertThat(maxIntDate.encodeAsSecondsSinceEpoch(5))
            .isEqualTo(byteArrayOf(127, -1, -1, -1))
    }
}
