/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime

private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
private const val SPINNER_DATE_TIME_FORMAT = "EEEE, MMMM dd"
private const val STATE_UI_DATE = "dd MMMM"
private const val SEVEN_AM = "7:00:00"

fun DateTime.toUtcIsoFormat(): String = toString(DATE_TIME_FORMAT)

fun DateTime?.toUiFormat(): String = this?.toString(STATE_UI_DATE) ?: "-"

fun LocalDate.toUiSpinnerFormat(): String = toString(SPINNER_DATE_TIME_FORMAT)

fun LocalDate.isMoreThan(days: Int, from: LocalDate): Boolean =
    !this
        .atSevenAm()
        .plusDays(days)
        .isAfter(from.atSevenAm())

fun latest(a: DateTime, b: DateTime) =
    if (a.isAfter(b)) a else b

fun LocalDate.atSevenAm(): DateTime =
    toDateTime(LocalTime.parse(SEVEN_AM))

fun LocalDate.after(count: Int): After =
    After(this, count)

fun DateTime.toUtc(): DateTime =
    toDateTime(DateTimeZone.UTC)

class After(val date: LocalDate, val count: Int) {
    fun days(): DateTime =
        date.plusDays(count).atSevenAm()

    fun day() = days()
}
