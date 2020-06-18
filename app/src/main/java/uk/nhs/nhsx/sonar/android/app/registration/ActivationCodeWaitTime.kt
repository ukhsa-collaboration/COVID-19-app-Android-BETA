/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import java.util.concurrent.TimeUnit

data class ActivationCodeWaitTime(
    val timeDelay: Long,
    val timeUnit: TimeUnit
)
