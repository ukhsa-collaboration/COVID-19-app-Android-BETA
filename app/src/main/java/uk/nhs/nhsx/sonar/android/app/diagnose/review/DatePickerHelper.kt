package uk.nhs.nhsx.sonar.android.app.diagnose.review

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate

fun localDateFromMidnightUtcTimestamp(timestamp: Long): LocalDate =
    DateTime(timestamp).withZone(UTC).toLocalDate()
