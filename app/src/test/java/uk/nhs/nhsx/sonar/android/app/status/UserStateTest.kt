/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.Symptom.ANOSMIA
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.atSevenAm
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import uk.nhs.nhsx.sonar.android.app.util.toUtc

class UserStateTest {

    private val exposedState = buildExposedState()
    private val symptomaticState = buildSymptomaticState()
    private val positiveState = buildPositiveState()

    private val expiredExposedState = buildExposedState(until = DateTime.now().minusSeconds(1))
    private val expiredSymptomaticState = buildSymptomaticState(until = DateTime.now().minusSeconds(1))
    private val expiredPositiveState = buildPositiveState(until = DateTime.now().minusSeconds(1))

    private val today = LocalDate(2020, 4, 10)

    @Test
    fun `exposed state factory method`() {
        val state = UserState.exposed(today)

        val thirteenDaysFromNowAt7 = DateTime(2020, 4, 23, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state).isEqualTo(ExposedState(today.atSevenAm().toUtc(), thirteenDaysFromNowAt7))
    }

    @Test
    fun `symptomatic state factory method - when symptoms started more than 7 days ago`() {
        val over7daysAgo = LocalDate(2020, 4, 2)
        val symptoms = nonEmptySetOf(COUGH, TEMPERATURE)
        val state = UserState.symptomatic(over7daysAgo, symptoms, today)

        val tomorrowAt7 = DateTime(2020, 4, 11, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.symptoms).isEqualTo(symptoms)
        assertThat(state.until).isEqualTo(tomorrowAt7)
    }

    @Test
    fun `symptomatic state factory method - when symptoms started less than 7 days ago`() {
        val lessThan7daysAgo = LocalDate(2020, 4, 5)
        val symptoms = nonEmptySetOf(TEMPERATURE)
        val state = UserState.symptomatic(lessThan7daysAgo, symptoms, today)

        val sevenDaysAfterSymptomsStart = DateTime(2020, 4, 12, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.until).isEqualTo(sevenDaysAfterSymptomsStart)
    }

    @Test
    fun `positive state factory method - when tested more than 7 days ago`() {
        val over7daysAgo = DateTime.parse("2020-04-02T11:11:11.000Z")
        val symptoms = nonEmptySetOf(COUGH, TEMPERATURE)
        val state = UserState.positive(over7daysAgo, symptoms, today)

        val tomorrowAt7 = DateTime(2020, 4, 11, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.symptoms).isEqualTo(symptoms)
        assertThat(state.until).isEqualTo(tomorrowAt7)
    }

    @Test
    fun `positive state factory method - when tested less than 7 days ago`() {
        val lessThan7daysAgo = DateTime.parse("2020-04-05T10:10:00.000Z")
        val symptoms = nonEmptySetOf(TEMPERATURE)
        val state = UserState.positive(lessThan7daysAgo, symptoms, today)

        val sevenDaysAfterTestDate = DateTime(2020, 4, 12, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.until).isEqualTo(sevenDaysAfterTestDate)
    }

    @Test
    fun `positive state factory method - with no symptom`() {
        val aDateTime = DateTime.parse("2020-04-02T11:11:11.000Z")
        val state = UserState.positive(aDateTime, emptySet(), today)

        assertThat(state.symptoms).isEmpty()
    }

    @Test
    fun `extend positive state`() {
        val symptoms = setOf(TEMPERATURE, COUGH, ANOSMIA)
        val tomorrowAt7am = DateTime(2020, 4, 11, 7, 0).toDateTime(DateTimeZone.UTC)

        assertThat(positiveState.extend(symptoms, today))
            .isEqualTo(
                PositiveState(
                    positiveState.since,
                    tomorrowAt7am,
                    symptoms
                )
            )
    }

    @Test
    fun `extend symptomatic state`() {
        val symptoms = setOf(TEMPERATURE, ANOSMIA)
        val tomorrowAt7am = DateTime(2020, 4, 11, 7, 0).toDateTime(DateTimeZone.UTC)

        assertThat(symptomaticState.extend(symptoms, today))
            .isEqualTo(
                SymptomaticState(
                    symptomaticState.since,
                    tomorrowAt7am,
                    NonEmptySet.create(symptoms)!!
                )
            )
    }

    @Test
    fun `extend other state`() {
        assertThat(exposedState.extend(emptySet())).isEqualTo(exposedState)
        assertThat(DefaultState.extend(emptySet())).isEqualTo(DefaultState)
    }

    @Test
    fun `test until`() {
        assertThat(DefaultState.until()).isNull()
        assertThat(exposedState.until()).isEqualTo(exposedState.until)
        assertThat(symptomaticState.until()).isEqualTo(symptomaticState.until)
        assertThat(positiveState.until()).isEqualTo(positiveState.until)
    }

    @Test
    fun `test hasExpired`() {
        assertThat(DefaultState.hasExpired()).isFalse()
        assertThat(exposedState.hasExpired()).isFalse()
        assertThat(symptomaticState.hasExpired()).isFalse()
        assertThat(positiveState.hasExpired()).isFalse()

        assertThat(expiredExposedState.hasExpired()).isTrue()
        assertThat(expiredSymptomaticState.hasExpired()).isTrue()
        assertThat(expiredPositiveState.hasExpired()).isTrue()
    }

    @Test
    fun `test displayState`() {
        assertThat(DefaultState.displayState()).isEqualTo(DisplayState.OK)
        assertThat(exposedState.displayState()).isEqualTo(DisplayState.AT_RISK)
        assertThat(symptomaticState.displayState()).isEqualTo(DisplayState.ISOLATE)
        assertThat(positiveState.displayState()).isEqualTo(DisplayState.ISOLATE)
    }

    @Test
    fun `test scheduleCheckInReminder`() {
        val reminders = mockk<Reminders>(relaxUnitFun = true)

        DefaultState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        exposedState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        symptomaticState.scheduleCheckInReminder(reminders)
        verify(exactly = 1) { reminders.scheduleCheckInReminder(symptomaticState.until) }
        clearMocks(reminders)

        positiveState.scheduleCheckInReminder(reminders)
        verify(exactly = 1) { reminders.scheduleCheckInReminder(positiveState.until) }
        clearMocks(reminders)

        expiredExposedState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredSymptomaticState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredPositiveState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }
    }

    @Test
    fun `test rescheduleCheckInReminder`() {
        val reminders = mockk<Reminders>(relaxUnitFun = true)

        DefaultState.rescheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.rescheduleCheckInReminder(any()) }

        exposedState.rescheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.rescheduleCheckInReminder(any()) }

        symptomaticState.rescheduleCheckInReminder(reminders)
        verify(exactly = 1) { reminders.rescheduleCheckInReminder(symptomaticState.until) }
        clearMocks(reminders)

        positiveState.rescheduleCheckInReminder(reminders)
        verify(exactly = 1) { reminders.rescheduleCheckInReminder(positiveState.until) }
        clearMocks(reminders)

        expiredExposedState.rescheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.rescheduleCheckInReminder(any()) }

        expiredSymptomaticState.rescheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.rescheduleCheckInReminder(any()) }

        expiredPositiveState.rescheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.rescheduleCheckInReminder(any()) }
    }

    @Test
    fun `test symptoms`() {
        assertThat(DefaultState.symptoms()).isEmpty()
        assertThat(exposedState.symptoms()).isEmpty()
        assertThat(symptomaticState.symptoms()).isEqualTo(symptomaticState.symptoms)
        assertThat(positiveState.symptoms()).isEqualTo(positiveState.symptoms)
    }
}
