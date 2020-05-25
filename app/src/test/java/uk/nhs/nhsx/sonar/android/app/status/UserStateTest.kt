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
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.util.atSevenAm
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import uk.nhs.nhsx.sonar.android.app.util.toUtc

class UserStateTest {

    private val exposedState = buildExposedState()
    private val symptomaticState = buildSymptomaticState()
    private val positiveState = buildPositiveState()
    private val checkinState = buildCheckinState()

    private val expiredExposedState = buildExposedState(until = DateTime.now().minusSeconds(1))
    private val expiredSymptomaticState = buildSymptomaticState(until = DateTime.now().minusSeconds(1))
    private val expiredPositiveState = buildPositiveState(until = DateTime.now().minusSeconds(1))
    private val expiredCheckinState = buildCheckinState(until = DateTime.now().minusSeconds(1))

    private val today = LocalDate(2020, 4, 10)

    @Test
    fun `exposed state factory method`() {
        val state = UserState.exposed(today)

        val thirteenDaysFromNowAt7 = DateTime(2020, 4, 23, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state).isEqualTo(ExposedState(today.atSevenAm().toUtc(), thirteenDaysFromNowAt7))
    }

    @Test
    fun `checkin state factory method`() {
        val symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        val symptomsDate = DateTime.now()
        val state = UserState.checkin(symptomsDate, symptoms, today)

        val tomorrowAt7 = DateTime(2020, 4, 11, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state).isEqualTo(CheckinState(symptomsDate, tomorrowAt7, symptoms))
    }

    @Test
    fun `symptomatic state factory method - when symptoms started more than 7 days ago`() {
        val over7daysAgo = LocalDate(2020, 4, 2)
        val symptoms = nonEmptySetOf(COUGH, Symptom.TEMPERATURE)
        val state = UserState.symptomatic(over7daysAgo, symptoms, today)

        val tomorrowAt7 = DateTime(2020, 4, 11, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.symptoms).isEqualTo(symptoms)
        assertThat(state.until).isEqualTo(tomorrowAt7)
    }

    @Test
    fun `symptomatic state factory method - when symptoms started less than 7 days ago`() {
        val lessThan7daysAgo = LocalDate(2020, 4, 5)
        val symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        val state = UserState.symptomatic(lessThan7daysAgo, symptoms, today)

        val sevenDaysAfterSymptomsStart = DateTime(2020, 4, 12, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.until).isEqualTo(sevenDaysAfterSymptomsStart)
    }

    @Test
    fun `positive state factory method - when tested more than 7 days ago`() {
        val over7daysAgo = DateTime.parse("2020-04-02T11:11:11.000Z")
        val symptoms = nonEmptySetOf(COUGH, Symptom.TEMPERATURE)
        val state = UserState.positive(over7daysAgo, symptoms, today)

        val tomorrowAt7 = DateTime(2020, 4, 11, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.symptoms).isEqualTo(symptoms)
        assertThat(state.until).isEqualTo(tomorrowAt7)
    }

    @Test
    fun `positive state factory method - when tested less than 7 days ago`() {
        val lessThan7daysAgo = DateTime.parse("2020-04-05T10:10:00.000Z")
        val symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        val state = UserState.positive(lessThan7daysAgo, symptoms, today)

        val sevenDaysAfterTestDate = DateTime(2020, 4, 12, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.until).isEqualTo(sevenDaysAfterTestDate)
    }

    @Test
    fun `test until`() {
        assertThat(DefaultState.until()).isNull()
        assertThat(exposedState.until()).isEqualTo(exposedState.until)
        assertThat(symptomaticState.until()).isEqualTo(symptomaticState.until)
        assertThat(positiveState.until()).isEqualTo(positiveState.until)
        assertThat(checkinState.until()).isEqualTo(checkinState.until)
    }

    @Test
    fun `test hasExpired`() {
        assertThat(DefaultState.hasExpired()).isFalse()
        assertThat(exposedState.hasExpired()).isFalse()
        assertThat(symptomaticState.hasExpired()).isFalse()
        assertThat(positiveState.hasExpired()).isFalse()
        assertThat(checkinState.hasExpired()).isFalse()

        assertThat(expiredExposedState.hasExpired()).isTrue()
        assertThat(expiredSymptomaticState.hasExpired()).isTrue()
        assertThat(expiredPositiveState.hasExpired()).isTrue()
        assertThat(expiredCheckinState.hasExpired()).isTrue()
    }

    @Test
    fun `test displayState`() {
        assertThat(DefaultState.displayState()).isEqualTo(DisplayState.OK)
        assertThat(exposedState.displayState()).isEqualTo(DisplayState.AT_RISK)
        assertThat(symptomaticState.displayState()).isEqualTo(DisplayState.ISOLATE)
        assertThat(positiveState.displayState()).isEqualTo(DisplayState.ISOLATE)
        assertThat(checkinState.displayState()).isEqualTo(DisplayState.ISOLATE)
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

        checkinState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredExposedState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredSymptomaticState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredPositiveState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredCheckinState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }
    }

    @Test
    fun `test symptoms`() {
        assertThat(DefaultState.symptoms()).isEmpty()
        assertThat(exposedState.symptoms()).isEmpty()
        assertThat(symptomaticState.symptoms()).isEqualTo(symptomaticState.symptoms)
        assertThat(positiveState.symptoms()).isEqualTo(positiveState.symptoms)
        assertThat(checkinState.symptoms()).isEqualTo(checkinState.symptoms)
    }
}
