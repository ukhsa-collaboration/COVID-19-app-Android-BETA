/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import androidx.work.ListenableWorker
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
        coEvery { registrationUseCase.register() } returns RegistrationResult.Success

        val result = sut.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        coVerify { registrationUseCase.register() }
    }

    @Test
    fun onError() = runBlockingTest {
        coEvery { registrationUseCase.register() } returns RegistrationResult.Error

        val result = sut.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
    }

    @Test
    fun onWaitingForActivationCode() = runBlockingTest {
        coEvery { registrationUseCase.register() } returns RegistrationResult.WaitingForActivationCode

        val result = sut.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

        val success = result as ListenableWorker.Result.Success
        val isWaitingForActivationCode =
            success.outputData.getBoolean(WAITING_FOR_ACTIVATION_CODE, false)
        assertTrue(isWaitingForActivationCode, "Output should contain WAITING_FOR_ACTIVATION_CODE")
    }
}
