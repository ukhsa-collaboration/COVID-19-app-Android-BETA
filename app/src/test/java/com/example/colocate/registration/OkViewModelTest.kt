package com.example.colocate.registration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.colocate.ViewState
import com.example.colocate.status.OkViewModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import net.lachlanmckee.timberjunit.TimberTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.util.concurrent.TimeoutException

@ExperimentalCoroutinesApi
class OkViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val logAllOnFailuresRule: TimberTestRule = TimberTestRule.logAllWhenTestFails()

    val testDispatcher = TestCoroutineDispatcher()

    private val registrationUseCase = mockk<RegistrationUseCase>()
    private val observer = mockk<Observer<ViewState>>(relaxed = true)

    @Before
    fun setUp() {
        Timber.plant(Timber.DebugTree())
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun registerSuccess() = runBlockingTest {
        coEvery { registrationUseCase.register() } returns RegistrationResult.Success

        val sut = OkViewModel(registrationUseCase)
        sut.viewState().observeForever(observer)
        sut.register()

        verifyOrder {
            observer.onChanged(ViewState.Progress)
            observer.onChanged(ViewState.Success)
        }
    }

    @Test
    fun registerFailure() = runBlockingTest {
        val exception = TimeoutException()
        coEvery { registrationUseCase.register() } returns RegistrationResult.Failure(exception)

        val sut = OkViewModel(registrationUseCase)
        sut.viewState().observeForever(observer)
        sut.register()

        testDispatcher.advanceTimeBy(2_000)

        verifyOrder {
            observer.onChanged(ViewState.Progress)
            observer.onChanged(ViewState.Error(exception))
        }
    }
}
