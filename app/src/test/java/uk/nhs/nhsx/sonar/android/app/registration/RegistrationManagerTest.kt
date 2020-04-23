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
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager.Companion.WORK_NAME

@ExperimentalCoroutinesApi
class RegistrationManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val dispatcher = Dispatchers.Unconfined
    private val workInfoLiveData = MutableLiveData<WorkInfo>()
    private val workInfo = mockk<WorkInfo>()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val sut = spyk(RegistrationManager(context, workManager, dispatcher))

    @Test
    fun registerEnqueuesWork() {
        sut.register()

        verify {
            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun registerCreatesWorkRequestWithInitialDelay() {
        sut.register(INITIAL_DELAY_IN_SECONDS)
        verify { sut.createWorkRequest(INITIAL_DELAY_IN_SECONDS) }
    }

    @Test
    fun createWorkRequest() {
        val workRequest = sut.createWorkRequest(INITIAL_DELAY_IN_SECONDS)
        assertThat(workRequest.workSpec.initialDelay).isEqualTo(INITIAL_DELAY_IN_MILLISECONDS)
    }

    @Test
    fun onSuccessWithoutDataStartsBluetoothService() = runBlockingTest {
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
        every { workManager.getWorkInfoByIdLiveData(any()) } returns workInfoLiveData
        every { workInfo.state } returns WorkInfo.State.SUCCEEDED
        every { workInfo.outputData } returns Data.Builder()
            .putBoolean(RegistrationWorker.WAITING_FOR_ACTIVATION_CODE, true).build()
        every { sut.scheduleRegisterRetryInOneHour() } returns Unit
        workInfoLiveData.value = workInfo

        sut.register()

        verify { sut.scheduleRegisterRetryInOneHour() }
    }

    companion object {
        const val INITIAL_DELAY_IN_SECONDS = 500L
        const val INITIAL_DELAY_IN_MILLISECONDS = INITIAL_DELAY_IN_SECONDS * 1_000
    }
}
