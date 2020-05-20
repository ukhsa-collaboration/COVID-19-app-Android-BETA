/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class UserStateSerializationTest {

    val serialize = UserStateSerialization::serialize
    val deserialize = UserStateSerialization::deserialize

    @Test
    fun `serialize default state`() {

        assertThat(serialize(DefaultState()))
            .isEqualTo("""{"type":"DefaultState","testResult":"null"}""")
    }

    @Test
    fun `serialize default state with test result`() {
        assertThat(
            serialize(
                DefaultState(
                    TestResult(
                        "NEGATIVE",
                        stateChanged = true,
                        dismissed = false
                    )
                )
            )
        )
            .isEqualTo("""{"type":"DefaultState","testResult":"{\"result\":\"NEGATIVE\",\"stateChanged\":true,\"dismissed\":false}"}""")
    }

    @Test
    fun `serialize recovery state`() {

        assertThat(serialize(RecoveryState()))
            .isEqualTo("""{"type":"RecoveryState","testResult":"null"}""")
    }

    @Test
    fun `serialize recovery state with test result`() {
        assertThat(
            serialize(
                RecoveryState(
                    TestResult(
                        "NEGATIVE",
                        stateChanged = true,
                        dismissed = false
                    )
                )
            )
        )
            .isEqualTo("""{"type":"RecoveryState","testResult":"{\"result\":\"NEGATIVE\",\"stateChanged\":true,\"dismissed\":false}"}""")
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
                """{"type":"AmberState","testResult":"null","until":1587241302262}"""
            )
    }

    @Test
    fun `serialize amber state with test result`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(
            serialize(
                AmberState(
                    until, TestResult(
                        "NEGATIVE",
                        stateChanged = true,
                        dismissed = false
                    )
                )
            )
        )
            .isEqualTo(
                """{"type":"AmberState","testResult":"{\"result\":\"NEGATIVE\",\"stateChanged\":true,\"dismissed\":false}","until":1587241302262}"""
            )
    }

    @Test
    fun `serialize red state`() {
        val until = DateTime(1587241302263L, UTC)
        val symptoms = nonEmptySetOf(Symptom.COUGH, Symptom.TEMPERATURE)

        assertThat(serialize(RedState(until, symptoms)))
            .isEqualTo(
                """{"symptoms":["COUGH","TEMPERATURE"],"until":1587241302263,"type":"RedState","testResult":"null","symptomsStartDate":-1}"""
            )
    }

    @Test
    fun `serialize red state with symptoms start date`() {
        val until = DateTime(1587241302263L, UTC)
        val symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        val symptomsStartDate = DateTime(1587241303263L, UTC)
        assertThat(serialize(RedState(until, symptoms, symptomsStartDate)))
            .isEqualTo(
                """{"symptoms":["TEMPERATURE"],"until":1587241302263,"type":"RedState","testResult":"null","symptomsStartDate":1587241303263}"""
            )
    }

    @Test
    fun `serialize checkin state`() {
        val until = DateTime(1587241302263L, UTC)
        val symptoms = nonEmptySetOf(Symptom.TEMPERATURE)

        assertThat(serialize(CheckinState(until, symptoms)))
            .isEqualTo(
                """{"symptoms":["TEMPERATURE"],"until":1587241302263,"type":"CheckinState","testResult":"null","symptomsStartDate":-1}"""
            )
    }

    @Test
    fun `serialize checkin state with symptoms start date`() {
        val until = DateTime(1587241302263L, UTC)
        val symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        val symptomsStartDate = DateTime(1587241303263L, UTC)

        assertThat(serialize(CheckinState(until, symptoms, symptomsStartDate)))
            .isEqualTo(
                """{"symptoms":["TEMPERATURE"],"until":1587241302263,"type":"CheckinState","testResult":"null","symptomsStartDate":1587241303263}"""
            )
    }

    @Test
    fun `deserialize null`() {
        assertThat(deserialize(null))
            .isEqualTo(DefaultState())
    }

    @Test
    fun `deserialize default state`() {
        assertThat(deserialize("""{"type":"DefaultState","testResult":"null"}"""))
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
    fun `deserialize ember(typo) state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"type":"EmberState"}"""))
            .isEqualTo(AmberState(until))
    }

    @Test
    fun `deserialize amber state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"type":"AmberState"}"""))
            .isEqualTo(AmberState(until))
        assertThat(deserialize("""{"type":"AmberState","testResult":"null","until":1587241302262}"""))
            .isEqualTo(AmberState(until))
    }

    @Test
    fun `deserialize amber state with test result`() {
        val until = DateTime(1587241302262L, UTC)

        val state = AmberState(
            until, TestResult(
                "NEGATIVE",
                stateChanged = true,
                dismissed = false
            )
        )
        assertThat(
            deserialize(
                """{
            "type":"AmberState",
            "testResult":"{\"result\":\"NEGATIVE\",\"stateChanged\":true,\"dismissed\":false}",
            "until":1587241302262
            }"""
            )
        )
            .isEqualTo(state)
    }

    @Test
    fun `deserialize red state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"symptoms":["COUGH"],"type":"RedState"}"""))
            .isEqualTo(RedState(until, nonEmptySetOf(Symptom.COUGH)))
    }

    @Test
    fun `deserialize red state with test result`() {
        val until = DateTime(1587241302262L, UTC)

        val state = RedState(
            until,
            nonEmptySetOf(Symptom.COUGH),
            testResult = TestResult(
                "NEGATIVE",
                stateChanged = true,
                dismissed = false
            )
        )
        assertThat(
            deserialize(
                """{
            "type":"RedState",
            "symptoms":["COUGH"],
            "testResult":"{\"result\":\"NEGATIVE\",\"stateChanged\":true,\"dismissed\":false}",
            "until":1587241302262
            }"""
            )
        )
            .isEqualTo(state)
    }

    @Test
    fun `deserialize red state with symptoms date`() {
        val until = DateTime(1587241302262L, UTC)

        val state = RedState(
            until,
            nonEmptySetOf(Symptom.COUGH),
            symptomsStartDate = until
        )
        assertThat(
            deserialize(
                """{
            "type":"RedState",
            "symptoms":["COUGH"],
            "symptomsStartDate":1587241302262,
            "until":1587241302262
            }"""
            )
        )
            .isEqualTo(state)
    }

    @Test
    fun `deserialize checkin state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(deserialize("""{"until":1587241302262,"symptoms":["COUGH"],"type":"CheckinState"}"""))
            .isEqualTo(CheckinState(until, nonEmptySetOf(Symptom.COUGH)))
    }

    @Test
    fun `deserialize invalid red state`() {
        assertThat(deserialize("""{"until":1587241302262,"symptoms":[],"type":"RedState"}"""))
            .isEqualTo(DefaultState())
    }
}
