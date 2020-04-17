package uk.nhs.nhsx.sonar.android.app.registration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.notifications.ReminderManager
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.status.OkViewModel
import java.util.concurrent.TimeoutException

@ExperimentalCoroutinesApi
class OkViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val logAllOnFailuresRule: TimberTestRule = TimberTestRule.logAllWhenTestFails()

    private val testDispatcher = TestCoroutineDispatcher()

    private val registrationUseCase = mockk<RegistrationUseCase>(relaxed = true)
    private val onboardingStatusProvider = mockk<OnboardingStatusProvider>(relaxed = true)
    private val sonarIdProvider = mockk<SonarIdProvider>(relaxed = true)
    private val reminderManager = mockk<ReminderManager>(relaxed = true)
    private val observer = mockk<Observer<ViewState>>(relaxed = true)
    private val sut =
        OkViewModel(registrationUseCase, onboardingStatusProvider, sonarIdProvider, reminderManager)

    @Before
    fun setUp() {
        Timber.plant(Timber.DebugTree())
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
    }

    @Test
    fun whenStartedForTheFirstTimeSetsOnboardingFinished() {
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { onboardingStatusProvider.isOnboardingFinished() } returns false

        sut.onStart()

        verify(exactly = 1) {
            onboardingStatusProvider.setOnboardingFinished(true)
        }
    }

    @Test
    fun whenStartedForTheFirstTimeStartsRegistration() {
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { onboardingStatusProvider.isOnboardingFinished() } returns false

        sut.onStart()

        coVerify(exactly = 1) {
            registrationUseCase.register()
        }
    }

    @Test
    fun whenStartedForTheSecondTimeOrLaterDoesNotSetOnboardingFinished() {
        every { onboardingStatusProvider.isOnboardingFinished() } returns true
        sut.viewState().observeForever(observer)

        sut.onStart()

        verify(exactly = 0) {
            onboardingStatusProvider.setOnboardingFinished(any())
        }
    }

    @Test
    fun whenRegisteredDoesNotStartRegistration() {
        every { sonarIdProvider.hasProperSonarId() } returns true
        sut.viewState().observeForever(observer)

        sut.onStart()

        coVerify(exactly = 0) {
            registrationUseCase.register()
        }
        verify {
            observer.onChanged(ViewState.Success)
        }
    }

    @Test
    fun whenStartedForTheSecondTimeOrLaterDoesNotStartsRegistration() {
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { onboardingStatusProvider.isOnboardingFinished() } returns true
        sut.viewState().observeForever(observer)

        sut.onStart()

        coVerify(exactly = 0) {
            registrationUseCase.register()
        }
        verify {
            observer.onChanged(ViewState.Error)
        }
    }

    @Test
    fun registerSuccess() = runBlockingTest {
        coEvery { registrationUseCase.register() } returns RegistrationResult.Success

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

        sut.viewState().observeForever(observer)
        sut.register()

        testDispatcher.advanceTimeBy(2_000)

        verifyOrder {
            observer.onChanged(ViewState.Progress)
            observer.onChanged(ViewState.Error)
        }
    }

    @Test
    fun onRegistrationFailureSetReminder() = runBlockingTest {
        val exception = TimeoutException()
        coEvery { registrationUseCase.register() } returns RegistrationResult.Failure(exception)

        sut.register()

        verify(exactly = 1) {
            reminderManager.scheduleReminder()
        }
    }

    @Test
    fun onRegistrationSuccessCancelReminder() = runBlockingTest {
        coEvery { registrationUseCase.register() } returns RegistrationResult.Success

        sut.register()

        verify(exactly = 1) {
            reminderManager.cancelReminder()
            reminderManager.hideReminderNotification()
        }
        verify(exactly = 0) {
            reminderManager.scheduleReminder()
        }
    }
}
