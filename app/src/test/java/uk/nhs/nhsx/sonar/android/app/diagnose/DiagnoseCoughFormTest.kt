/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.AmberState
import uk.nhs.nhsx.sonar.android.app.status.CheckinState
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.RecoveryState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class DiagnoseCoughFormTest {

    private val stateStorage = mockk<UserStateStorage>(relaxed = true)
    private val form = DiagnoseCoughForm(stateStorage)

    @Test
    fun `initial state is blue then final state is blue`() {
        every { stateStorage.get() } returns DefaultState

        val result = form.submit(emptySet())

        assertThat(result).isEqualTo(StateResult.Close)
    }

    @Test
    fun `initial state is blue then final state is red`() {
        every { stateStorage.get() } returns DefaultState

        val result = form.submit(setOf(TEMPERATURE))

        assertThat(result).isEqualTo(StateResult.Review)
    }

    @Test
    fun `initial state is red then final state Is blue`() {
        val expected = DefaultState
        every { stateStorage.get() } returns RedState(DateTime.now(UTC), nonEmptySetOf(COUGH))

        val result = form.submit(emptySet())

        assertThat(result).isEqualTo(StateResult.Main(expected))
    }

    @Test
    fun `initial state is red then final state is recovery`() {
        val expected = RecoveryState
        every { stateStorage.get() } returns RedState(DateTime.now(UTC), nonEmptySetOf(COUGH))

        val result = form.submit(setOf(COUGH))

        assertThat(result).isEqualTo(StateResult.Main(expected))
    }

    @Test
    fun `initial state is red then final state is checkin`() {
        val tomorrowSevenAm = LocalDate.now()
            .plusDays(1)
            .toDateTime(LocalTime("7:00:00"))
            .toDateTime(UTC)

        val expected = CheckinState(tomorrowSevenAm, nonEmptySetOf(TEMPERATURE))
        every { stateStorage.get() } returns RedState(DateTime.now(UTC), nonEmptySetOf(COUGH))

        val result = form.submit(setOf(TEMPERATURE))

        assertThat(result).isEqualTo(StateResult.Main(expected))
    }

    @Test
    fun `initial state is checkin then final state is checkin`() {
        val tomorrowSevenAm = LocalDate.now()
            .plusDays(1)
            .toDateTime(LocalTime("7:00:00"))
            .toDateTime(UTC)

        val expected = CheckinState(tomorrowSevenAm, nonEmptySetOf(TEMPERATURE))
        every { stateStorage.get() } returns CheckinState(DateTime.now(UTC), nonEmptySetOf(COUGH))

        val result = form.submit(setOf(TEMPERATURE))

        assertThat(result).isEqualTo(StateResult.Main(expected))
    }

    @Test
    fun `initial state is Amber then final state Is red`() {
        RedState(DateTime.now(UTC).plusDays(7), nonEmptySetOf(TEMPERATURE))
        every { stateStorage.get() } returns AmberState(DateTime.now(UTC))

        val result = form.submit(setOf(TEMPERATURE))

        assertThat(result).isEqualTo(StateResult.Review)
    }

    @Test
    fun `initial state is Amber then final state Is Amber`() {
        every { stateStorage.get() } returns AmberState(DateTime.now(UTC))

        val result = form.submit(emptySet())

        assertThat(result).isEqualTo(StateResult.Close)
    }
}
