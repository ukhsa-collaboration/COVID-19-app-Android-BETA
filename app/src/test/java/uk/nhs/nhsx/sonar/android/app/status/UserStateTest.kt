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
    private val redState = buildRedState()
    private val checkinState = buildCheckinState()

    private val expiredExposedState = buildExposedState(until = DateTime.now().minusSeconds(1))
    private val expiredRedState = buildRedState(until = DateTime.now().minusSeconds(1))
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
    fun `red state factory method - when symptoms started more than 7 days ago`() {
        val over7daysAgo = LocalDate(2020, 4, 2)
        val symptoms = nonEmptySetOf(COUGH, Symptom.TEMPERATURE)
        val state = UserState.red(over7daysAgo, symptoms, today)

        val tomorrowAt7 = DateTime(2020, 4, 11, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.symptoms).isEqualTo(symptoms)
        assertThat(state.until).isEqualTo(tomorrowAt7)
    }

    @Test
    fun `red state factory method - when symptoms started less than 7 days ago`() {
        val lessThan7daysAgo = LocalDate(2020, 4, 5)
        val symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        val state = UserState.red(lessThan7daysAgo, symptoms, today)

        val sevenDaysAfterSymptomsStart = DateTime(2020, 4, 12, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.until()).isEqualTo(sevenDaysAfterSymptomsStart)
    }

    @Test
    fun `test until`() {
        assertThat(DefaultState.until()).isNull()
        assertThat(RecoveryState.until()).isNull()
        assertThat(exposedState.until()).isEqualTo(exposedState.until)
        assertThat(redState.until()).isEqualTo(redState.until)
        assertThat(checkinState.until()).isEqualTo(checkinState.until)
    }

    @Test
    fun `test hasExpired`() {
        assertThat(DefaultState.hasExpired()).isFalse()
        assertThat(RecoveryState.hasExpired()).isFalse()
        assertThat(exposedState.hasExpired()).isFalse()
        assertThat(redState.hasExpired()).isFalse()
        assertThat(checkinState.hasExpired()).isFalse()

        assertThat(expiredExposedState.hasExpired()).isTrue()
        assertThat(expiredRedState.hasExpired()).isTrue()
        assertThat(expiredCheckinState.hasExpired()).isTrue()
    }

    @Test
    fun `test displayState`() {
        assertThat(DefaultState.displayState()).isEqualTo(DisplayState.OK)
        assertThat(RecoveryState.displayState()).isEqualTo(DisplayState.OK)
        assertThat(exposedState.displayState()).isEqualTo(DisplayState.AT_RISK)
        assertThat(redState.displayState()).isEqualTo(DisplayState.ISOLATE)
        assertThat(checkinState.displayState()).isEqualTo(DisplayState.ISOLATE)
    }

    @Test
    fun `test scheduleCheckInReminder`() {
        val reminders = mockk<Reminders>(relaxUnitFun = true)

        DefaultState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        RecoveryState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        exposedState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        redState.scheduleCheckInReminder(reminders)
        verify(exactly = 1) { reminders.scheduleCheckInReminder(redState.until) }
        clearMocks(reminders)

        checkinState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredExposedState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredRedState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredCheckinState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }
    }

    @Test
    fun `test symptoms`() {
        assertThat(DefaultState.symptoms()).isEmpty()
        assertThat(RecoveryState.symptoms()).isEmpty()
        assertThat(exposedState.symptoms()).isEmpty()
        assertThat(redState.symptoms()).isEqualTo(redState.symptoms)
        assertThat(checkinState.symptoms()).isEqualTo(checkinState.symptoms)
    }
}
