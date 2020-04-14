package uk.nhs.nhsx.sonar.android.app.util

import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime

private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

fun LocalDateTime.toUtcIsoFormat(): String =
    this.toDateTime(DateTimeZone.UTC).toString(DATE_TIME_FORMAT)
