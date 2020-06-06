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
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import uk.nhs.nhsx.sonar.android.app.util.toUtcNormalized

class UserStateTest {

    private val exposedState = buildExposedState()
    private val symptomaticState = buildSymptomaticState()
    private val exposedSymptomaticState = buildExposedSymptomaticState()
    private val positiveState = buildPositiveState()

    private val expiredExposedState = buildExposedState(until = DateTime.now().minusSeconds(1))
    private val expiredSymptomaticState =
        buildSymptomaticState(until = DateTime.now().minusSeconds(1))
    private val expiredExposedSymptomaticState =
        buildExposedSymptomaticState(until = DateTime.now().minusSeconds(1))
    private val expiredPositiveState = buildPositiveState(until = DateTime.now().minusSeconds(1))

    private val today = LocalDate(2020, 4, 10)

    @Test
    fun `exposed state factory method with exposure date`() {
        val state = UserState.exposed(today)

        val fourteenDaysFromNowAt7 = DateTime(2020, 4, 24, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state).isEqualTo(ExposedState(today.toUtcNormalized(), fourteenDaysFromNowAt7))
    }

    @Test
    fun `exposed state factory method with exposed symptomatic state`() {
        val originalState = buildExposedSymptomaticState()
        val state = UserState.exposed(originalState)

        assertThat(state).isEqualTo(
            ExposedState(
                since = originalState.since,
                until = originalState.until
            )
        )
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
    fun `exposedSymptomatic state factory method`() {
        val aSymptomDate = LocalDate(2020, 4, 5)
        val symptoms = nonEmptySetOf(TEMPERATURE)
        val exposed = buildExposedState()

        val state = UserState.exposedSymptomatic(aSymptomDate, exposed, symptoms)

        assertThat(state.since).isEqualTo(aSymptomDate.toUtcNormalized())
        assertThat(state.until).isEqualTo(exposed.until)
        assertThat(state.symptoms).isEqualTo(symptoms)
    }

    @Test
    fun `positive state factory method - when tested more than 7 days ago`() {
        val over7daysAgo = DateTime.parse("2020-04-02T11:11:11.000Z")
        val state = UserState.positive(over7daysAgo, today)

        val tomorrowAt7 = DateTime(2020, 4, 11, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.symptoms).isEmpty()
        assertThat(state.until).isEqualTo(tomorrowAt7)
    }

    @Test
    fun `positive state factory method - when tested less than 7 days ago`() {
        val lessThan7daysAgo = DateTime.parse("2020-04-05T10:10:00.000Z")
        val state = UserState.positive(lessThan7daysAgo, today)

        val sevenDaysAfterTestDate = DateTime(2020, 4, 12, 7, 0).toDateTime(DateTimeZone.UTC)
        assertThat(state.until).isEqualTo(sevenDaysAfterTestDate)
    }

    @Test
    fun `positive state factory method - with symptomatic state and symptoms started less than 7 days ago`() {
        val symptomatic = buildSymptomaticState().let {
            it.copy(since = it.since.minusDays(3))
        }
        val state = UserState.positive(symptomatic)

        assertThat(state).isEqualTo(
            PositiveState(
                since = symptomatic.since,
                until = symptomatic.since.plusDays(UserState.NUMBER_OF_DAYS_IN_SYMPTOMATIC),
                symptoms = symptomatic.symptoms
            )
        )
    }

    @Test
    fun `positive state factory method - with symptomatic state and symptoms started more than 7 days ago`() {
        val symptomatic = buildSymptomaticState().let {
            it.copy(since = it.since.minusDays(10))
        }
        val state = UserState.positive(symptomatic)

        assertThat(state).isEqualTo(
            PositiveState(
                since = symptomatic.since,
                until = LocalDate.now().toUtcNormalized(),
                symptoms = symptomatic.symptoms
            )
        )
    }

    @Test
    fun `positive state factory method - with exposed-symptomatic state and symptoms started less than 7 days ago`() {
        val exposedSymptomatic = buildExposedSymptomaticState().let {
            it.copy(since = it.since.minusDays(3))
        }
        val state = UserState.positive(exposedSymptomatic)

        assertThat(state).isEqualTo(
            PositiveState(
                since = exposedSymptomatic.since,
                until = exposedSymptomatic.since.plusDays(UserState.NUMBER_OF_DAYS_IN_SYMPTOMATIC),
                symptoms = exposedSymptomatic.symptoms
            )
        )
    }

    @Test
    fun `positive state factory method - with exposed-symptomatic state and symptoms started more than 7 days ago`() {
        val exposedSymptomatic = buildExposedSymptomaticState().let {
            it.copy(since = it.since.minusDays(10))
        }
        val state = UserState.positive(exposedSymptomatic)

        assertThat(state).isEqualTo(
            PositiveState(
                since = exposedSymptomatic.since,
                until = LocalDate.now().toUtcNormalized(),
                symptoms = exposedSymptomatic.symptoms
            )
        )
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
    fun `extend exposed symptomatic state`() {
        val symptoms = setOf(TEMPERATURE, ANOSMIA)
        val tomorrowAt7am = DateTime(2020, 4, 11, 7, 0).toDateTime(DateTimeZone.UTC)

        assertThat(exposedSymptomaticState.extend(symptoms, today))
            .isEqualTo(
                ExposedSymptomaticState(
                    exposedSymptomaticState.since,
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
    fun `expire states with expiry date`() {
        assertThat(positiveState.expire()).isEqualTo(positiveState.copy(until = yesterday()))
        assertThat(symptomaticState.expire()).isEqualTo(symptomaticState.copy(until = yesterday()))
        assertThat(exposedSymptomaticState.expire()).isEqualTo(exposedSymptomaticState.copy(until = yesterday()))
        assertThat(exposedState.expire()).isEqualTo(exposedState.copy(until = yesterday()))
    }

    @Test
    fun `expire other state`() {
        assertThat(DefaultState.expire()).isEqualTo(DefaultState)
    }

    @Test
    fun `test until`() {
        assertThat(DefaultState.until()).isNull()
        assertThat(exposedState.until()).isEqualTo(exposedState.until)
        assertThat(symptomaticState.until()).isEqualTo(symptomaticState.until)
        assertThat(exposedSymptomaticState.until()).isEqualTo(exposedSymptomaticState.until)
        assertThat(positiveState.until()).isEqualTo(positiveState.until)
    }

    @Test
    fun `test hasExpired`() {
        assertThat(DefaultState.hasExpired()).isFalse()
        assertThat(exposedState.hasExpired()).isFalse()
        assertThat(symptomaticState.hasExpired()).isFalse()
        assertThat(exposedSymptomaticState.hasExpired()).isFalse()
        assertThat(positiveState.hasExpired()).isFalse()

        assertThat(expiredExposedState.hasExpired()).isTrue()
        assertThat(expiredSymptomaticState.hasExpired()).isTrue()
        assertThat(expiredExposedSymptomaticState.hasExpired()).isTrue()
        assertThat(expiredPositiveState.hasExpired()).isTrue()
    }

    @Test
    fun `test displayState`() {
        assertThat(DefaultState.displayState()).isEqualTo(DisplayState.OK)
        assertThat(exposedState.displayState()).isEqualTo(DisplayState.AT_RISK)
        assertThat(symptomaticState.displayState()).isEqualTo(DisplayState.ISOLATE)
        assertThat(exposedSymptomaticState.displayState()).isEqualTo(DisplayState.ISOLATE)
        assertThat(positiveState.displayState()).isEqualTo(DisplayState.ISOLATE)
    }

    @Test
    fun `scheduleCheckInReminder - when states are not expired`() {
        val reminders = mockk<Reminders>(relaxUnitFun = true)

        symptomaticState.scheduleCheckInReminder(reminders)
        verify { reminders.cancelCheckinReminder() }
        verify(exactly = 1) { reminders.scheduleCheckInReminder(symptomaticState.until) }
        clearMocks(reminders)

        exposedSymptomaticState.scheduleCheckInReminder(reminders)
        verify { reminders.cancelCheckinReminder() }
        verify(exactly = 1) { reminders.scheduleCheckInReminder(exposedSymptomaticState.until) }
        clearMocks(reminders)

        positiveState.scheduleCheckInReminder(reminders)
        verify { reminders.cancelCheckinReminder() }
        verify(exactly = 1) { reminders.scheduleCheckInReminder(positiveState.until) }
        clearMocks(reminders)
    }

    @Test
    fun `scheduleCheckInReminder - when states are expired`() {
        val reminders = mockk<Reminders>(relaxUnitFun = true)

        expiredSymptomaticState.scheduleCheckInReminder(reminders)
        verify { reminders.cancelCheckinReminder() }
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredExposedSymptomaticState.scheduleCheckInReminder(reminders)
        verify { reminders.cancelCheckinReminder() }
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredPositiveState.scheduleCheckInReminder(reminders)
        verify { reminders.cancelCheckinReminder() }
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }
    }

    @Test
    fun `scheduleCheckInReminder - for exposed state`() {
        val reminders = mockk<Reminders>(relaxUnitFun = true)

        exposedState.scheduleCheckInReminder(reminders)
        verify { reminders.cancelCheckinReminder() }
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }
    }

    @Test
    fun `scheduleCheckInReminder - for default state`() {
        val reminders = mockk<Reminders>(relaxUnitFun = true)

        DefaultState.scheduleCheckInReminder(reminders)
        verify { reminders.cancelCheckinReminder() }
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }
    }

    @Test
    fun `test symptoms`() {
        assertThat(DefaultState.symptoms()).isEmpty()
        assertThat(exposedState.symptoms()).isEmpty()
        assertThat(symptomaticState.symptoms()).isEqualTo(symptomaticState.symptoms)
        assertThat(positiveState.symptoms()).isEqualTo(positiveState.symptoms)
    }

    private fun yesterday() = LocalDate.now().minusDays(1).toUtcNormalized()
}
