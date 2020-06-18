/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TokenRefreshWorkScheduler @Inject constructor(private val workManager: WorkManager) {

    fun schedule(sonarId: String, newToken: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request =
            OneTimeWorkRequestBuilder<TokenRefreshWorker>()
                .setConstraints(constraints)
                .setInputData(TokenRefreshWork.data(sonarId, newToken))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

        workManager.enqueue(request)
    }
}
