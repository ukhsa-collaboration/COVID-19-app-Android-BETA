package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager.Companion.ACTIVATION_CODE_TIMED_OUT
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager.Companion.REGISTRATION_WORK
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class RegistrationManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val dispatcher = Dispatchers.Unconfined
    private val workInfo = mockk<WorkInfo>()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val sut = spyk(RegistrationManager(context, workManager, dispatcher))

    @Test
    fun registerEnqueuesWork() {
        sut.register()

        verify {
            workManager.enqueueUniqueWork(
                REGISTRATION_WORK,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun registerCreatesWorkRequestWithInitialDelay() {
        sut.register(INITIAL_DELAY_IN_SECONDS)
        verify { sut.createWorkRequest(INITIAL_DELAY_IN_SECONDS, activationCodeTimedOut = false) }
    }

    @Test
    fun createWorkRequest() {
        val workRequest = sut.createWorkRequest(INITIAL_DELAY_IN_SECONDS, activationCodeTimedOut = false)
        val workSpec = workRequest.workSpec
        assertThat(workSpec.initialDelay).isEqualTo(INITIAL_DELAY_IN_MILLISECONDS)
        assertThat(workSpec.input.getBoolean(ACTIVATION_CODE_TIMED_OUT, true)).isFalse()
    }

    @Test
    fun onSuccessWithoutDataStartsBluetoothService() = runBlockingTest {
        val workInfoLiveData = MutableLiveData<WorkInfo>()
        every { workManager.getWorkInfoByIdLiveData(any()) } returns workInfoLiveData
        every { workInfo.state } returns WorkInfo.State.SUCCEEDED
        every { workInfo.outputData } returns Data.EMPTY
        workInfoLiveData.value = workInfo

        mockkObject(BluetoothService.Companion)
        every { BluetoothService.start(context) } returns Unit

        sut.register()

        verify { BluetoothService.start(context) }
    }

    @Test
    fun onSuccessWithDataSchedulesRegisterRetryInOneHour() = runBlockingTest {
        val workInfoLiveData = MutableLiveData<WorkInfo>()
        every { workManager.getWorkInfoByIdLiveData(any()) } returnsMany listOf(
            workInfoLiveData,
            MutableLiveData()
        )
        every { workInfo.state } returns WorkInfo.State.SUCCEEDED
        every { workInfo.outputData } returns Data.Builder()
            .putBoolean(RegistrationWorker.WAITING_FOR_ACTIVATION_CODE, true).build()
        workInfoLiveData.value = workInfo
        val workRequest = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(
                REGISTRATION_WORK,
                ExistingWorkPolicy.REPLACE,
                capture(workRequest)
            )
        } returns mockk(relaxed = true)

        sut.register()

        verify(exactly = 2) {
            workManager.enqueueUniqueWork(
                REGISTRATION_WORK,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
        val capturedRequest = workRequest.captured
        val workSpec = capturedRequest.workSpec

        assertEquals(ONE_HOUR_IN_MILLIS, workSpec.initialDelay, "WorkRequest initial delay is not one hour")
        assertThat(workSpec.input.getBoolean(ACTIVATION_CODE_TIMED_OUT, false)).isTrue()
    }

    companion object {
        const val INITIAL_DELAY_IN_SECONDS = 500L
        const val INITIAL_DELAY_IN_MILLISECONDS = INITIAL_DELAY_IN_SECONDS * 1_000
        const val ONE_HOUR_IN_MILLIS = 60L * 60 * 1_000
    }
}
