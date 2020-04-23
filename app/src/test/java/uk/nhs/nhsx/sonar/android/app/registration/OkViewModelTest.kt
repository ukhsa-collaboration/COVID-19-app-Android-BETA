/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import net.lachlanmckee.timberjunit.TimberTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.status.OkViewModel

@ExperimentalCoroutinesApi
class OkViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val logAllOnFailuresRule: TimberTestRule = TimberTestRule.logAllWhenTestFails()

    private val testDispatcher = TestCoroutineDispatcher()

    private val onboardingStatusProvider = mockk<OnboardingStatusProvider>(relaxed = true)
    private val sonarIdProvider = mockk<SonarIdProvider>(relaxed = true)
    private val registrationManager = mockk<RegistrationManager>(relaxed = true)
    private val observer = mockk<Observer<ViewState>>(relaxed = true)
    private val sut =
        OkViewModel(onboardingStatusProvider, sonarIdProvider, registrationManager)

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
    fun onStartSetsOnboardingFinished() {
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { onboardingStatusProvider.isOnboardingFinished() } returns false

        sut.onStart()

        verify(exactly = 1) {
            onboardingStatusProvider.setOnboardingFinished(true)
        }
    }

    @Test
    fun onStartStartsRegistration() {
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { onboardingStatusProvider.isOnboardingFinished() } returns false

        sut.onStart()

        coVerify(exactly = 1) {
            registrationManager.register()
        }
    }

    @Test
    fun onStartIfAlreadyRegisteredSkipsRegistration() {
        every { sonarIdProvider.hasProperSonarId() } returns true
        every { onboardingStatusProvider.isOnboardingFinished() } returns false

        sut.onStart()

        coVerify(exactly = 0) {
            registrationManager.register()
        }
    }
}
