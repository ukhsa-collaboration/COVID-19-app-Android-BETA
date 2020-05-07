package uk.nhs.nhsx.sonar.android.app.http

import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime

interface UTCClock {
    fun now(): LocalDateTime = LocalDateTime.now(DateTimeZone.UTC)
}

class RealUTCClock : UTCClock
