/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test

class RedStateExpiryTest {

    private val now = DateTime.parse("2020-04-20T11:00:00.000+01:00")

    @Before
    fun setUp() {
        DateTimeUtils.setCurrentMillisFixed(now.millis)
    }

    @After
    internal fun tearDown() {
        DateTimeUtils.setCurrentMillisSystem()
    }

    @Test
    fun `when symptoms date is today, state is valid until 7 days`() {
        val state = RedStateFactory.normal(LocalDate.now(), setOf(Symptom.COUGH))

        assertThat(state.until).isEqualTo(DateTime.parse("2020-04-26T05:00:00.000Z"))
    }

    @Test
    fun `when symptoms date is yesterday, state is valid until 6 days`() {
        val state = RedStateFactory.normal(LocalDate.now().minusDays(1), setOf(Symptom.COUGH))

        assertThat(state.until).isEqualTo(DateTime.parse("2020-04-25T05:00:00.000Z"))
    }

    @Test
    fun `when symptoms date is 7 days ago, state is valid until tomorrow`() {
        val state = RedStateFactory.normal(LocalDate.now().minusDays(7), setOf(Symptom.COUGH))

        assertThat(state.until).isEqualTo(DateTime.parse("2020-04-21T05:00:00.000Z"))
    }

    @Test
    fun `when extending the state, state is valid until tomorrow`() {
        val state = RedStateFactory.extended(Symptom.COUGH)

        assertThat(state.until).isEqualTo(DateTime.parse("2020-04-21T05:00:00.000Z"))
    }
}
