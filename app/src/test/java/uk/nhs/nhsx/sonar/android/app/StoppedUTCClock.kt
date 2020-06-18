/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import org.joda.time.LocalDateTime
import uk.nhs.nhsx.sonar.android.app.http.UTCClock

class StoppedUTCClock(private val alwaysNow: LocalDateTime) :
    UTCClock {
    override fun now(): LocalDateTime {
        return alwaysNow
    }
}
