package uk.nhs.nhsx.sonar.android.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val DAY_IN_MILLIS = 24L * 60 * 60 * 1000

fun Date.toIsoFormat(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK).let {
        it.timeZone = TimeZone.getTimeZone("UTC")
        it.format(this)
    }

fun Date.daysAgo(days: Int): Date {
    return Date(this.time - days * DAY_IN_MILLIS)
}
