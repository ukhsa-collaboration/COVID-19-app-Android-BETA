/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.inbox

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult.NEGATIVE
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult.POSITIVE
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult.INVALID

class TestInfoSerializationTest {

    val serialize = TestInfoSerialization::serialize
    val deserialize = TestInfoSerialization::deserialize

    @Test
    fun `serialize test information`() {
        val testDate = DateTime(15872413022578L, UTC)

        assertThat(serialize(TestInfo(NEGATIVE, testDate)))
            .isEqualTo("""{"result":"NEGATIVE","date":15872413022578}""")
    }

    @Test
    fun `serialize another test information`() {
        val testDate = DateTime(15872413000000L, UTC)

        assertThat(serialize(TestInfo(POSITIVE, testDate)))
            .isEqualTo("""{"result":"POSITIVE","date":15872413000000}""")
    }

    @Test
    fun `deserialize null test information`() {
        assertThat(deserialize(null)).isNull()
    }

    @Test
    fun `deserialize test information`() {
        assertThat(deserialize("""{"result":"INVALID","date":15872413022578}"""))
            .isEqualTo(TestInfo(INVALID, DateTime(15872413022578L, UTC)))
    }
}
