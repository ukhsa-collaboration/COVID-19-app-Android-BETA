/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NO_DAYS_IN_RED
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class UserStateSerializationTest {

    val serialize = UserStateSerialization::serialize
    val deserialize = UserStateSerialization::deserialize

    @Test
    fun `serialize default state`() {

        assertThat(serialize(DefaultState()))
            .isEqualTo("""{"type":"DefaultState","testInfo":"null"}""")
    }

    @Test
    fun `serialize default state with test result`() {
        val testDate = DateTime(15872413022578L, UTC)
        assertThat(
            serialize(
                DefaultState(
                    TestInfo(
                        TestResult.NEGATIVE,
                        testDate,
                        stateChanged = true,
                        dismissed = false
                    )
                )
            )
        )
            .isEqualTo("""{"type":"DefaultState","testInfo":"{\"dismissed\":false,\"testResult\":\"NEGATIVE\",\"stateChanged\":true,\"testDate\":15872413022578}"}""")
    }

    @Test
    fun `serialize recovery state`() {

        assertThat(serialize(RecoveryState()))
            .isEqualTo("""{"type":"RecoveryState","testInfo":"null"}""")
    }

    @Test
    fun `serialize recovery state with test result`() {
        val testDate = DateTime(15872413022578L, UTC)
        assertThat(
            serialize(
                RecoveryState(
                    TestInfo(
                        TestResult.NEGATIVE,
                        testDate,
                        stateChanged = true,
                        dismissed = false
                    )
                )
            )
        )
            .isEqualTo("""{"type":"RecoveryState","testInfo":"{\"dismissed\":false,\"testResult\":\"NEGATIVE\",\"stateChanged\":true,\"testDate\":15872413022578}"}""")
    }

    @Test
    fun `serialize amber state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(
            serialize(
                AmberState(
                    until
                )
            )
        )
            .isEqualTo(
                """{"type":"AmberState","testInfo":"null","until":1587241302262}"""
            )
    }

    @Test
    fun `serialize amber state with test result`() {
        val until = DateTime(1587241302262L, UTC)
        val testDate = DateTime(15872413022578L, UTC)

        assertThat(
            serialize(
                AmberState(
                    until, TestInfo(
                        TestResult.NEGATIVE,
                        testDate,
                        stateChanged = true,
                        dismissed = false
                    )
                )
            )
        )
            .isEqualTo(
                """{"type":"AmberState","testInfo":"{\"dismissed\":false,\"testResult\":\"NEGATIVE\",\"stateChanged\":true,\"testDate\":15872413022578}","until":1587241302262}"""
            )
    }

    @Test
    fun `serialize red state`() {
        val since = DateTime(1387241302263L, UTC)
        val until = DateTime(1587241302263L, UTC)
        val symptoms = nonEmptySetOf(Symptom.COUGH, Symptom.TEMPERATURE)

        assertThat(serialize(RedState(since, until, symptoms)))
            .isEqualTo(
                """{"symptoms":["COUGH","TEMPERATURE"],"testInfo":"null","until":1587241302263,"type":"RedState","since":1387241302263}"""
            )
    }

    @Test
    fun `serialize red state with symptoms start date`() {
        val since = DateTime(1587241303263L, UTC)
        val until = DateTime(1587241302263L, UTC)
        val symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        assertThat(serialize(RedState(since, until, symptoms)))
            .isEqualTo(
                """{"symptoms":["TEMPERATURE"],"testInfo":"null","until":1587241302263,"type":"RedState","since":1587241303263}"""
            )
    }

    @Test
    fun `serialize checkin state`() {
        val since = DateTime(1387241302263L, UTC)
        val until = DateTime(1587241302263L, UTC)
        val symptoms = nonEmptySetOf(Symptom.TEMPERATURE)

        assertThat(serialize(CheckinState(since, until, symptoms)))
            .isEqualTo(
                """{"symptoms":["TEMPERATURE"],"testInfo":"null","until":1587241302263,"type":"CheckinState","since":1387241302263}"""
            )
    }

    @Test
    fun `serialize checkin state with symptoms start date`() {
        val since = DateTime(1587241303263L, UTC)
        val until = DateTime(1587241302263L, UTC)
        val symptoms = nonEmptySetOf(Symptom.TEMPERATURE)

        assertThat(serialize(CheckinState(since, until, symptoms)))
            .isEqualTo(
                """{"symptoms":["TEMPERATURE"],"testInfo":"null","until":1587241302263,"type":"CheckinState","since":1587241303263}"""
            )
    }

    @Test
    fun `deserialize null`() {
        assertThat(deserialize(null))
            .isEqualTo(DefaultState())
    }

    @Test
    fun `deserialize default state`() {
        assertThat(deserialize("""{"type":"DefaultState","testInfo":"null"}"""))
            .isEqualTo(DefaultState())
    }

    @Test
    fun `deserialize default state - with legacy until timestamp`() {
        assertThat(deserialize("""{"until":1587241302261,"type":"DefaultState"}"""))
            .isEqualTo(DefaultState())
    }

    @Test
    fun `deserialize recovery state`() {
        assertThat(deserialize("""{"type":"RecoveryState"}"""))
            .isEqualTo(RecoveryState())
    }

    @Test
    fun `deserialize recovery state - with legacy until timestamp`() {
        assertThat(deserialize("""{"until":1587241302262,"type":"RecoveryState"}"""))
            .isEqualTo(RecoveryState())
    }

    @Test
    fun `deserialize legacy ember(typo) state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"type":"EmberState"}"""))
            .isEqualTo(AmberState(until))
    }

    @Test
    fun `deserialize amber state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"type":"AmberState"}"""))
            .isEqualTo(AmberState(until))
        assertThat(deserialize("""{"type":"AmberState","testInfo":"null","until":1587241302262}"""))
            .isEqualTo(AmberState(until))
    }

    @Test
    fun `deserialize amber state with test result`() {
        val until = DateTime(1587241302262L, UTC)
        val testDate = DateTime(15872413022578L, UTC)

        val state = AmberState(
            until, TestInfo(
                TestResult.NEGATIVE,
                testDate,
                stateChanged = true,
                dismissed = false
            )
        )
        assertThat(
            deserialize(
                """{
            "type":"AmberState",
            "testInfo":"{\"testResult\":\"NEGATIVE\",\"testDate\":15872413022578,\"stateChanged\":true,\"dismissed\":false}",
            "until":1587241302262
            }"""
            )
        )
            .isEqualTo(state)
    }

    @Test
    fun `deserialize red state without symptom date`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"symptoms":["COUGH"],"type":"RedState"}"""))
            .isEqualTo(RedState(until.minusDays(NO_DAYS_IN_RED), until, nonEmptySetOf(Symptom.COUGH)))
    }

    @Test
    fun `deserialize red state with test result`() {
        val since = DateTime(1387241302262L, UTC)
        val until = DateTime(1587241302262L, UTC)
        val testDate = DateTime(15872413022578L, UTC)

        val state = RedState(
            since,
            until,
            nonEmptySetOf(Symptom.COUGH),
            testInfo = TestInfo(
                TestResult.NEGATIVE,
                testDate,
                stateChanged = true,
                dismissed = false
            )
        )
        assertThat(
            deserialize(
                """{
            "type":"RedState",
            "since":1387241302262,
            "until":1587241302262,
            "symptoms":["COUGH"],
            "testInfo":"{\"testResult\":\"NEGATIVE\",\"testDate\":15872413022578,\"stateChanged\":true,\"dismissed\":false}"
            }"""
            )
        )
            .isEqualTo(state)
    }

    @Test
    fun `deserialize red state with symptoms date`() {
        val since = DateTime(1587241300000L, UTC)
        val until = DateTime(1587241302262L, UTC)

        val state = RedState(
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
            "type":"RedState"
            }"""
            )
        )
            .isEqualTo(state)
    }

    @Test
    fun `deserialize checkin state without symptom date`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"symptoms":["COUGH"],"type":"CheckinState"}"""))
            .isEqualTo(CheckinState(until.minusDays(NO_DAYS_IN_RED), until, nonEmptySetOf(Symptom.COUGH)))
    }

    @Test
    fun `deserialize checkin state with symptom date`() {
        val since = DateTime(1587241302262L, UTC)
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"since":1587241302262,"until":1587241302262,"symptoms":["COUGH"],"type":"CheckinState"}"""))
            .isEqualTo(CheckinState(since, until, nonEmptySetOf(Symptom.COUGH)))
    }

    @Test
    fun `deserialize invalid red state`() {
        assertThat(deserialize("""{"until":1587241302262,"symptoms":[],"type":"RedState"}"""))
            .isEqualTo(DefaultState())
    }
}
