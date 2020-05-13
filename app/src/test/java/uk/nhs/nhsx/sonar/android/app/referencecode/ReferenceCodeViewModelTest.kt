/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.Promise.Deferred
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeViewModel.State.Loaded
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeViewModel.State.Loading
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider

@ExperimentalCoroutinesApi
class ReferenceCodeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val referenceCodeApi = mockk<ReferenceCodeApi>()
    private val testObserver = mockk<Observer<ReferenceCodeViewModel.State>>(relaxed = true)

    private val viewModel = ReferenceCodeViewModel(
        testDispatcher,
        referenceCodeApi,
        sonarIdProvider
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel.state().observeForever(testObserver)
    }

    @Test
    fun `test state() when fetch successful`() {
        val deferred = Deferred<ReferenceCode>()

        every { sonarIdProvider.get() } returns SONAR_ID
        every { referenceCodeApi.get(SONAR_ID) } returns deferred.promise
        every { sonarIdProvider.hasProperSonarId() } returns true
        viewModel.getReferenceCode()

        deferred.resolve(ReferenceCode("some-other-code-201"))

        verifyOrder {
            testObserver.onChanged(Loading)
            testObserver.onChanged(Loaded(ReferenceCode("some-other-code-201")))
        }
    }

    @Test
    fun `test state() when and fetch fails`() {
        val deferred = Deferred<ReferenceCode>()

        every { sonarIdProvider.get() } returns SONAR_ID
        every { referenceCodeApi.get(SONAR_ID) } returns deferred.promise
        every { sonarIdProvider.hasProperSonarId() } returns true

        viewModel.getReferenceCode()

        deferred.fail("Oops")

        verifyOrder {
            testObserver.onChanged(Loading)
            testObserver.onChanged(ReferenceCodeViewModel.State.Error)
        }
    }

    @Test
    fun `test state() when sonar id is not available`() {
        every { sonarIdProvider.hasProperSonarId() } returns false

        viewModel.getReferenceCode()

        verifyOrder {
            testObserver.onChanged(Loading)
            testObserver.onChanged(ReferenceCodeViewModel.State.Error)
        }

        verify { referenceCodeApi wasNot Called }
    }

    @After
    fun cleanUp() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    companion object {
        private const val SONAR_ID = "::another valid sonar id::"
    }
}
