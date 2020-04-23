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
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager.Companion.WORK_NAME
import java.util.UUID

class RegistrationManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val dispatcher = Dispatchers.Unconfined
    private val workInfoLiveData = MutableLiveData<WorkInfo>()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val sut = spyk(RegistrationManager(context, workManager, dispatcher))

    @Test
    fun tryRegister() {
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
    fun getWorkInfoByIdLiveData() = runBlockingTest {
        workInfoLiveData.value = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.SUCCEEDED,
            Data.EMPTY,
            listOf(),
            Data.EMPTY,
            0
        )
        every { workManager.getWorkInfoByIdLiveData(any()) } returns workInfoLiveData

        sut.register()
        //BluetoothService.start(context)
        mockkObject(BluetoothService.Companion)
        //mockkStatic(BluetoothService.Companion::class)
        every { BluetoothService.start(any()) } returns Unit
        //verify { context.getString(any()) }
        verify { BluetoothService.start(any()) }
    }

}
