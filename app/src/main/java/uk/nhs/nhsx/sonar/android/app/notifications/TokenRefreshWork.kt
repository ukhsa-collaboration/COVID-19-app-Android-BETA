package uk.nhs.nhsx.sonar.android.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import uk.nhs.nhsx.sonar.android.app.appComponent
import javax.inject.Inject

class TokenRefreshWorker(appContext: Context, private val params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val work by lazy { appComponent.tokenRefreshWork() }

    override suspend fun doWork(): Result =
        work.doWork(params.inputData)
}

class TokenRefreshWork @Inject constructor(private val api: NotificationTokenApi) {

    companion object {
        fun data(sonarId: String, newToken: String) =
            workDataOf("sonarId" to sonarId, "newToken" to newToken)

        val Data.sonarId: String get() = getString("sonarId")!!
        val Data.newToken: String get() = getString("newToken")!!
    }

    suspend fun doWork(inputData: Data): ListenableWorker.Result {
        val sonarId = inputData.sonarId
        val newToken = inputData.newToken

        return api
            .updateToken(sonarId, newToken)
            .toCoroutine()
            .map { ListenableWorker.Result.success() }
            .orElse { ListenableWorker.Result.retry() }
    }
}
