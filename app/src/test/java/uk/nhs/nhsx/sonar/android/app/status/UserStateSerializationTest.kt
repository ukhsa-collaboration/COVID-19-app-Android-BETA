/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NO_DAYS_IN_EXPOSED
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NO_DAYS_IN_SYMPTOMATIC
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class UserStateSerializationTest {

    val serialize = UserStateSerialization::serialize
    val deserialize = UserStateSerialization::deserialize

    @Test
    fun `serialize default state`() {

        assertThat(serialize(DefaultState))
            .isEqualTo("""{"type":"DefaultState"}""")
    }

    @Test
    fun `serialize exposed state`() {
        val since = DateTime(1387241302262L, UTC)
        val until = DateTime(1587241302262L, UTC)

        assertThat(serialize(ExposedState(since, until)))
            .isEqualTo(
                """{"type":"ExposedState","until":1587241302262,"since":1387241302262}"""
            )
    }

    @Test
    fun `serialize symptomatic state`() {
        val since = DateTime(1387241302263L, UTC)
        val until = DateTime(1587241302263L, UTC)
        val symptoms = nonEmptySetOf(Symptom.COUGH, Symptom.TEMPERATURE)

        assertThat(serialize(SymptomaticState(since, until, symptoms)))
            .isEqualTo(
                """{"symptoms":["COUGH","TEMPERATURE"],"until":1587241302263,"type":"SymptomaticState","since":1387241302263}"""
            )
    }

    @Test
    fun `serialize positive state`() {
        val since = DateTime(1387241302263L, UTC)
        val until = DateTime(1587241302263L, UTC)
        val symptoms = nonEmptySetOf(Symptom.COUGH, Symptom.TEMPERATURE)

        assertThat(serialize(PositiveState(since, until, symptoms)))
            .isEqualTo(
                """{"symptoms":["COUGH","TEMPERATURE"],"until":1587241302263,"type":"PositiveState","since":1387241302263}"""
            )
    }

    @Test
    fun `serialize positive state with no sympotoms`() {
        val since = DateTime(1387241302263L, UTC)
        val until = DateTime(1587241302263L, UTC)

        assertThat(serialize(PositiveState(since, until, emptySet())))
            .isEqualTo(
                """{"symptoms":[],"until":1587241302263,"type":"PositiveState","since":1387241302263}"""
            )
    }

    @Test
    fun `deserialize null`() {
        assertThat(deserialize(null)).isEqualTo(DefaultState)
    }

    @Test
    fun `deserialize default state`() {
        assertThat(deserialize("""{"type":"DefaultState","testInfo":"null"}"""))
            .isEqualTo(DefaultState)
    }

    @Test
    fun `deserialize default state - with legacy until timestamp`() {
        assertThat(deserialize("""{"until":1587241302261,"type":"DefaultState"}"""))
            .isEqualTo(DefaultState)
    }

    @Test
    fun `deserialize legacy ember(typo) state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"type":"EmberState"}"""))
            .isEqualTo(ExposedState(until.minusDays(NO_DAYS_IN_EXPOSED), until))
    }

    @Test
    fun `deserialize legacy amber state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"type":"AmberState"}"""))
            .isEqualTo(ExposedState(until.minusDays(NO_DAYS_IN_EXPOSED), until))
    }

    @Test
    fun `deserialize exposed state without symptom date`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"type":"ExposedState"}"""))
            .isEqualTo(ExposedState(until.minusDays(NO_DAYS_IN_EXPOSED), until))
    }

    @Test
    fun `deserialize exposed state with symptom date`() {
        val since = DateTime(1587241300000L, UTC)
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"since":1587241300000,"until":1587241302262,"type":"ExposedState"}"""))
            .isEqualTo(ExposedState(since, until))
    }

    @Test
    fun `deserialize legacy red state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"symptoms":["COUGH"],"type":"RedState"}"""))
            .isEqualTo(SymptomaticState(until.minusDays(NO_DAYS_IN_SYMPTOMATIC), until, nonEmptySetOf(Symptom.COUGH)))
    }

    @Test
    fun `deserialize symptomatic state without symptom date`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"symptoms":["COUGH"],"type":"SymptomaticState"}"""))
            .isEqualTo(SymptomaticState(until.minusDays(NO_DAYS_IN_SYMPTOMATIC), until, nonEmptySetOf(Symptom.COUGH)))
    }

    @Test
    fun `deserialize symptomatic state with symptoms date`() {
        val since = DateTime(1587241300000L, UTC)
        val until = DateTime(1587241302262L, UTC)

        val state = SymptomaticState(
            since,
            until,
            nonEmptySetOf(Symptom.COUGH)
        )
        assertThat(
            deserialize(
                """{
            "since":1587241300000,
            "until":1587241302262,
            "symptoms":["COUGH"],
            "type":"SymptomaticState"
            }"""
            )
        )
            .isEqualTo(state)
    }

    @Test
    fun `deserialize positive state`() {
        val since = DateTime(1587241300000L, UTC)
        val until = DateTime(1587241302262L, UTC)

        val state = PositiveState(
            since,
            until,
            nonEmptySetOf(Symptom.COUGH)
        )
        assertThat(
            deserialize(
                """{
            "since":1587241300000,
            "until":1587241302262,
            "symptoms":["COUGH"],
            "type":"PositiveState"
            }"""
            )
        )
            .isEqualTo(state)
    }

    @Test
    fun `deserialize positive state without symptoms`() {
        val since = DateTime(1587241300000L, UTC)
        val until = DateTime(1587241302262L, UTC)

        val state = PositiveState(since, until, emptySet())

        assertThat(
            deserialize(
                """{
            "since":1587241300000,
            "until":1587241302262,
            "type":"PositiveState"
            }"""
            )
        )
            .isEqualTo(state)
    }

    @Test
    fun `deserialize positive state with empty symptoms`() {
        val since = DateTime(1587241300000L, UTC)
        val until = DateTime(1587241302262L, UTC)

        val state = PositiveState(since, until, emptySet())

        assertThat(
            deserialize(
                """{
            "since":1587241300000,
            "until":1587241302262,
            "symptoms":[],
            "type":"PositiveState"
            }"""
            )
        )
            .isEqualTo(state)
    }

    @Test
    fun `deserialize legacy checkin state without symptom date`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"symptoms":["COUGH"],"type":"CheckinState"}"""))
            .isEqualTo(SymptomaticState(until.minusDays(NO_DAYS_IN_SYMPTOMATIC), until, nonEmptySetOf(Symptom.COUGH)))
    }

    @Test
    fun `deserialize legacy checkin state with symptom date`() {
        val since = DateTime(1587241302262L, UTC)
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"since":1587241302262,"until":1587241302262,"symptoms":["COUGH"],"type":"CheckinState"}"""))
            .isEqualTo(SymptomaticState(since, until, nonEmptySetOf(Symptom.COUGH)))
    }

    @Test
    fun `deserialize invalid symptomatic state`() {
        assertThat(deserialize("""{"until":1587241302262,"symptoms":[],"type":"SymptomaticState"}"""))
            .isEqualTo(DefaultState)
    }
}
