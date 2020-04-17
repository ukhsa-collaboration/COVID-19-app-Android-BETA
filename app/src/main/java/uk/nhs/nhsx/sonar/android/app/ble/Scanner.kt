/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import kotlinx.coroutines.CoroutineScope

interface Scanner {
    fun start(coroutineScope: CoroutineScope)
    fun stop()
}
