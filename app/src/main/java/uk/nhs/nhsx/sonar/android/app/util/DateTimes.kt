/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import org.joda.time.DateTime
import org.joda.time.LocalDate

private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
private const val SPINNER_DATE_TIME_FORMAT = "EEEE, MMMM dd"
private const val STATE_UI_DATE = "dd MMMM"

fun DateTime.toUtcIsoFormat(): String = toString(DATE_TIME_FORMAT)

fun DateTime.toUiFormat(): String = toString(STATE_UI_DATE)

fun LocalDate.toUiSpinnerFormat(): String = toString(SPINNER_DATE_TIME_FORMAT)
