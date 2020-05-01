package uk.nhs.nhsx.sonar.android.app.registration

import androidx.work.ListenableWorker
import androidx.work.workDataOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationWorker.Companion.WAITING_FOR_ACTIVATION_CODE
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class RegistrationWorkTest {

    private val registrationUseCase = mockk<RegistrationUseCase>()
    private val sut = RegistrationWork(registrationUseCase)

    @Test
    fun onSuccess() = runBlockingTest {
        coEvery { registrationUseCase.register(any()) } returns RegistrationResult.Success

        val result = sut.doWork(workDataOf("foo" to "bar"))

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        coVerify { registrationUseCase.register(workDataOf("foo" to "bar")) }
    }

    @Test
    fun onError() = runBlockingTest {
        coEvery { registrationUseCase.register(any()) } returns RegistrationResult.Error

        val result = sut.doWork(workDataOf())

        assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
    }

    @Test
    fun onWaitingForActivationCode() = runBlockingTest {
        coEvery { registrationUseCase.register(any()) } returns RegistrationResult.WaitingForActivationCode

        val result = sut.doWork(workDataOf())

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

        val success = result as ListenableWorker.Result.Success
        val isWaitingForActivationCode =
            success.outputData.getBoolean(WAITING_FOR_ACTIVATION_CODE, false)
        assertTrue(isWaitingForActivationCode, "Output should contain WAITING_FOR_ACTIVATION_CODE")
    }
}
