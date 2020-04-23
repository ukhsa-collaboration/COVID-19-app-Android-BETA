/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.junit.Test

class RedStateExpiryTest {

    private val today = LocalDate(2020, 4, 20)

    @Test
    fun `when symptoms date is today, state is valid until 7 days`() {
        val state = RedStateFactory.normal(today, setOf(Symptom.COUGH), today)

        assertThat(state.until).isEqualTo(DateTime(2020, 4, 26, 7, 0).toDateTime(UTC))
    }

    @Test
    fun `when symptoms date is yesterday, state is valid until 6 days`() {
        val state = RedStateFactory.normal(today.minusDays(1), setOf(Symptom.COUGH), today)

        assertThat(state.until).isEqualTo(DateTime(2020, 4, 25, 7, 0).toDateTime(UTC))
    }

    @Test
    fun `when symptoms date is 7 days ago, state is valid until tomorrow`() {
        val state = RedStateFactory.normal(today.minusDays(7), setOf(Symptom.COUGH), today)

        assertThat(state.until).isEqualTo(DateTime(2020, 4, 21, 7, 0).toDateTime(UTC))
    }

    @Test
    fun `when extending the state, state is valid until tomorrow`() {
        val state = RedStateFactory.extended(Symptom.COUGH, today = today)

        assertThat(state.until).isEqualTo(DateTime(2020, 4, 21, 7, 0).toDateTime(UTC))
    }
}
