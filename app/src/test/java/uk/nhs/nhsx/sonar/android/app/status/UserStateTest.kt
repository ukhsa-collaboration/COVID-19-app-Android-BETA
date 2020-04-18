/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.junit.Test

class UserStateTest {

    @Test
    fun `serialize default state`() {
        val until = DateTime(1587241302261L, UTC)

        assertThat(DefaultState(until).serialize())
            .isEqualTo("""{"until":1587241302261,"type":"DefaultState"}""")
    }

    @Test
    fun `serialize ember state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(EmberState(until).serialize())
            .isEqualTo("""{"until":1587241302262,"type":"EmberState"}""")
    }

    @Test
    fun `serialize red state`() {
        val until = DateTime(1587241302263L, UTC)
        val symptoms = setOf(Symptom.COUGH, Symptom.TEMPERATURE)

        assertThat(RedState(until, symptoms).serialize())
            .isEqualTo("""{"until":1587241302263,"symptoms":["COUGH","TEMPERATURE"],"type":"RedState"}""")
    }

    @Test
    fun `deserialize default state`() {
        val until = DateTime(1587241302261L, UTC)

        assertThat(UserState.deserialize("""{"until":1587241302261,"type":"DefaultState"}"""))
            .isEqualTo(DefaultState(until))
    }

    @Test
    fun `deserialize ember state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(UserState.deserialize("""{"until":1587241302262,"type":"EmberState"}"""))
            .isEqualTo(EmberState(until))
    }

    @Test
    fun `deserialize red state`() {
        val until = DateTime(1587241302262L, UTC)

        assertThat(UserState.deserialize("""{"until":1587241302262,"symptoms":["COUGH"],"type":"RedState"}"""))
            .isEqualTo(RedState(until, setOf(Symptom.COUGH)))
    }
}
