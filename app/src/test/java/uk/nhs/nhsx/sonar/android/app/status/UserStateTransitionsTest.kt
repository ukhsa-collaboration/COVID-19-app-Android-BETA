/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.Symptom.ANOSMIA
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.NAUSEA
import uk.nhs.nhsx.sonar.android.app.status.Symptom.SNEEZE
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.diagnose
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.diagnoseForCheckin
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.expireExposedState
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.isSymptomatic
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.transitionOnContactAlert
import uk.nhs.nhsx.sonar.android.app.util.atSevenAm
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import uk.nhs.nhsx.sonar.android.app.util.toUtc

class UserStateTransitionsTest {

    private val today = LocalDate(2020, 4, 10)
    private val symptomsWithoutTemperature = nonEmptySetOf(COUGH)
    private val symptomsWithTemperature = nonEmptySetOf(TEMPERATURE)

    @Before
    fun setUp() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.now().millis)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, and no temperature`() {
        val sevenDaysAgoOrMore = today.minusDays(7)

        val state = diagnose(DefaultState, sevenDaysAgoOrMore, symptomsWithoutTemperature, today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, no temperature, and current state is Exposed`() {
        val exposedState = buildExposedState()
        val sevenDaysAgoOrMore = today.minusDays(7)

        val state = diagnose(exposedState, sevenDaysAgoOrMore, symptomsWithoutTemperature, today)

        assertThat(state).isEqualTo(exposedState)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, with temperature`() {
        val sevenDaysAgoOrMore = today.minusDays(7)
        val sevenDaysAfterSymptoms = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnose(
            DefaultState,
            sevenDaysAgoOrMore,
            symptomsWithTemperature,
            today
        )

        assertThat(state).isEqualTo(
            SymptomaticState(
                sevenDaysAgoOrMore.atSevenAm().toUtc(),
                sevenDaysAfterSymptoms,
                symptomsWithTemperature)
        )
    }

    @Test
    fun `diagnose - when symptoms date is less than 7 days ago, and no temperature`() {
        val lessThanSevenDaysAgo = today.minusDays(6)
        val sevenDaysAfterSymptoms = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnose(
            DefaultState,
            lessThanSevenDaysAgo,
            symptomsWithoutTemperature,
            today
        )

        assertThat(state).isEqualTo(
            SymptomaticState(
                lessThanSevenDaysAgo.atSevenAm().toUtc(),
                sevenDaysAfterSymptoms,
                symptomsWithoutTemperature
            )
        )
    }

    @Test
    fun `diagnose - when symptoms date is less than 7 days ago, with temperature`() {
        val lessThanSevenDaysAgo = today.minusDays(6)
        val sevenDaysAfterSymptoms = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnose(
            DefaultState,
            lessThanSevenDaysAgo,
            symptomsWithTemperature,
            today
        )

        assertThat(state).isEqualTo(
            SymptomaticState(
                lessThanSevenDaysAgo.atSevenAm().toUtc(),
                sevenDaysAfterSymptoms,
                symptomsWithTemperature
            )
        )
    }

    @Test
    fun `diagnose - when current state is exposed`() {
        val anySymptomDate = today.minusDays(6)

        val exposed = UserState.exposed(today)

        val state = diagnose(
            exposed,
            anySymptomDate,
            symptomsWithTemperature,
            today
        )

        assertThat(state)
            .isEqualTo(
                ExposedSymptomaticState(
                    exposed.since,
                    exposed.until,
                    symptomsWithTemperature
                )
            )
    }

    @Test
    fun `diagnoseForCheckin - with temperature`() {
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val currentState = UserState.symptomatic(today, nonEmptySetOf(COUGH))

        val state = diagnoseForCheckin(currentState, setOf(TEMPERATURE), today)

        assertThat(state).isEqualTo(SymptomaticState(currentState.since, tomorrow, nonEmptySetOf(TEMPERATURE)))
    }

    @Test
    fun `diagnoseForCheckin - with cough and temperature`() {
        val aDateTime = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val currentState = UserState.positive(aDateTime)

        val state = diagnoseForCheckin(currentState, setOf(COUGH, TEMPERATURE), today)

        assertThat(state).isEqualTo(PositiveState(currentState.since, tomorrow, nonEmptySetOf(COUGH, TEMPERATURE)))
    }

    @Test
    fun `diagnoseForCheckin - with cough`() {
        val aDateTime = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)
        val currentState = UserState.positive(aDateTime)

        val state = diagnoseForCheckin(currentState, setOf(COUGH), today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `diagnoseForCheckin - with anosmia`() {
        val currentState = UserState.symptomatic(today, nonEmptySetOf(TEMPERATURE))

        val state = diagnoseForCheckin(currentState, setOf(ANOSMIA), today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `diagnoseForCheckin - with no symptoms`() {
        val aDateTime = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)
        val currentState = UserState.positive(aDateTime)

        val state = diagnoseForCheckin(currentState, emptySet(), today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `test transitionOnContactAlert passes exposure date`() {
        val exposureDate = DateTime.parse("2020-04-21T16:00Z")
        val newState = transitionOnContactAlert(DefaultState, exposureDate)

        assertThat(newState).isInstanceOf(ExposedState::class.java)
        val exposedState = newState as ExposedState

        assertThat(exposedState.since.toLocalDate()).isEqualTo(exposureDate.toLocalDate())
    }

    @Test
    fun `test transitionOnContactAlert does not change state for exposed or symptomatic`() {
        assertThat(transitionOnContactAlert(buildExposedState(), DateTime.now())).isNull()
        assertThat(transitionOnContactAlert(buildSymptomaticState(), DateTime.now())).isNull()
    }

    @Test
    fun `test expireExposedState`() {
        val exposedState = buildExposedState()
        val symptomaticState = buildSymptomaticState()

        val expiredExposedState = buildExposedState(until = DateTime.now().minusSeconds(1))
        val expiredSymptomaticState = buildSymptomaticState(until = DateTime.now().minusSeconds(1))

        assertThat(expireExposedState(DefaultState)).isEqualTo(DefaultState)
        assertThat(expireExposedState(exposedState)).isEqualTo(exposedState)
        assertThat(expireExposedState(symptomaticState)).isEqualTo(symptomaticState)
        assertThat(expireExposedState(expiredSymptomaticState)).isEqualTo(expiredSymptomaticState)

        assertThat(expireExposedState(expiredExposedState)).isEqualTo(DefaultState)
    }

    @Test
    fun `isSymptomatic - with cough, temperature or loss of smell`() {
        assertThat(isSymptomatic(setOf(COUGH))).isTrue()
        assertThat(isSymptomatic(setOf(TEMPERATURE))).isTrue()
        assertThat(isSymptomatic(setOf(ANOSMIA))).isTrue()
    }

    @Test
    fun `isSymptomatic - with anything other than cough, temperature or loss of smell`() {
        assertThat(isSymptomatic(setOf(NAUSEA, SNEEZE))).isFalse()
    }

    @After
    fun tearDown() {
        DateTimeUtils.setCurrentMillisSystem()
    }
}
