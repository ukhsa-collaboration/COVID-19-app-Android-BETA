package uk.nhs.nhsx.sonar.android.app.notifications

import androidx.work.ListenableWorker
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.Promise

@ExperimentalCoroutinesApi
class TokenRefreshWorkTest {

    private val api = mockk<NotificationTokenApi>()
    private val work = TokenRefreshWork(api)
    private val inputData = TokenRefreshWork.data("my-sonar-id", "token-v3")

    @Test
    fun `test doWork - on success`() = runBlockingTest {
        val successDeferred = Promise.Deferred<Unit>().apply { resolve(Unit) }
        every { api.updateToken(any(), any()) } returns successDeferred.promise

        val result = work.doWork(inputData)

        verify { api.updateToken("my-sonar-id", "token-v3") }
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `test doWork - on api failure`() = runBlockingTest {
        val failedDeferred = Promise.Deferred<Unit>().apply { fail("Oops") }
        every { api.updateToken(any(), any()) } returns failedDeferred.promise

        val result = work.doWork(inputData)

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }
}
