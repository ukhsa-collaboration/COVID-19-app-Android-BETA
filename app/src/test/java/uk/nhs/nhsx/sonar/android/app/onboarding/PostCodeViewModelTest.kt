/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics
import uk.nhs.nhsx.sonar.android.app.analytics.partialPostcodeProvided
import uk.nhs.nhsx.sonar.android.app.util.observeEventForever

class PostCodeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val observer = mockk<Observer<PostCodeViewState>>(relaxed = true)
    private val navigationObserver = mockk<Observer<PostCodeNavigation>>(relaxed = true)
    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val analytics = mockk<SonarAnalytics>(relaxed = true)

    private val sut = PostCodeViewModel(postCodeProvider, analytics)

    @Test
    fun emptyPostCode() {
        sut.onContinue("")

        sut.viewState().observeForever(observer)
        verify(exactly = 1) {
            observer.onChanged(PostCodeViewState.Invalid)
        }
        verify(exactly = 0) {
            navigationObserver.onChanged(any())
            postCodeProvider.setPostCode(any())
        }
    }

    @Test
    fun invalidPostCode() {
        sut.onContinue("A")

        sut.viewState().observeForever(observer)
        verifyAll {
            observer.onChanged(PostCodeViewState.Invalid)
            navigationObserver wasNot Called
            postCodeProvider wasNot Called
        }
    }

    @Test
    fun validPostCodePrefix() {
        val validPostCode = "SW15"
        sut.onContinue(validPostCode)

        sut.viewState().observeForever(observer)
        sut.navigation().observeEventForever { navigationObserver.onChanged(it) }

        verifyAll {
            observer.onChanged(PostCodeViewState.Valid)
            navigationObserver.onChanged(PostCodeNavigation.Permissions)
            postCodeProvider.setPostCode(validPostCode)
            analytics.trackEvent(partialPostcodeProvided())
        }
    }
}
