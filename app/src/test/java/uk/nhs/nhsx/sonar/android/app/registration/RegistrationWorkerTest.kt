package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.android.volley.ClientError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationWorker.Companion.ACTIVATION_CODE_NOT_VALID
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationWorker.Companion.WAITING_FOR_ACTIVATION_CODE
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class RegistrationWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val registrationUseCase = mockk<RegistrationUseCase>()
    private val appComponent = mockk<ApplicationComponent>(relaxed = true)

    private val sut = RegistrationWorker(context, workerParams)

    @Before
    fun setUp() {
        mockkStatic("uk.nhs.nhsx.sonar.android.app.ColocateApplicationKt")
        every { sut.appComponent } returns appComponent
        sut.registrationUseCase = registrationUseCase
    }

    @Test
    fun onSuccessReturnsSuccess() = runBlockingTest {
        coEvery { registrationUseCase.register() } returns RegistrationResult.Success

        val result = sut.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }

    @Test
    fun onAlreadyRegisteredReturnsSuccess() = runBlockingTest {
        coEvery { registrationUseCase.register() } returns RegistrationResult.AlreadyRegistered

        val result = sut.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }

    @Test
    fun onFailureReturnsRetry() = runBlockingTest {
        coEvery { registrationUseCase.register() } returns RegistrationResult.Failure(
            RuntimeException()
        )

        val result = sut.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
    }

    @Test
    fun onActivationCodeNotValidFailureReturnsFailure() = runBlockingTest {
        coEvery { registrationUseCase.register() } returns RegistrationResult.ActivationCodeNotValidFailure(
            ClientError()
        )

        val result = sut.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        val failure = result as ListenableWorker.Result.Failure
        val isActivationCodeNotValid =
            failure.outputData.getBoolean(ACTIVATION_CODE_NOT_VALID, false)
        assertTrue(isActivationCodeNotValid, "Output should contain ACTIVATION_CODE_NOT_VALID")
    }

    @Test
    fun onWaitingForActivationCodeReturnsSuccess() = runBlockingTest {
        coEvery { registrationUseCase.register() } returns RegistrationResult.WaitingForActivationCode

        val result = sut.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        val success = result as ListenableWorker.Result.Success
        val isWaitingForActivationCode =
            success.outputData.getBoolean(WAITING_FOR_ACTIVATION_CODE, false)
        assertTrue(isWaitingForActivationCode, "Output should contain WAITING_FOR_ACTIVATION_CODE")
    }
}