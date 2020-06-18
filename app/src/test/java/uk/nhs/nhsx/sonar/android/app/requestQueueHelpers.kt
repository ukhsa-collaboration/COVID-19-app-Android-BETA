/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import com.android.volley.ExecutorDelivery
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.NoCache
import uk.nhs.nhsx.sonar.android.app.diagnose.OkHttpStack
import java.util.concurrent.Executors

fun testQueue(): RequestQueue =
    RequestQueue(
        NoCache(),
        BasicNetwork(OkHttpStack()),
        1,
        ExecutorDelivery(Executors.newSingleThreadExecutor())
    ).apply { start() }
