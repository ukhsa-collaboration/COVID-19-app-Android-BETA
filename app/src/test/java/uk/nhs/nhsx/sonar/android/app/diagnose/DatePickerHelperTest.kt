package uk.nhs.nhsx.sonar.android.app.diagnose

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.diagnose.review.localDateFromMidnightUtcTimestamp

class DatePickerHelperTest {

    @Test
    fun `test localDateFromMidnightUtcTimestamp()`() {
        val timestamp = DateTime.parse("2020-04-24T00:00:00Z").millis
        val expectedDate = LocalDate(2020, 4, 24)

        val actualDate = localDateFromMidnightUtcTimestamp(timestamp)

        assertThat(actualDate).isEqualTo(expectedDate)
    }
}
