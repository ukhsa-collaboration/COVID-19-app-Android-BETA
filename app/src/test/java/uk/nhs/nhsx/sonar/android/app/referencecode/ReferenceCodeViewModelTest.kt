/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.Promise.Deferred
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeViewModel.State.Loaded
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeViewModel.State.Loading

class ReferenceCodeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val api = mockk<ReferenceCodeApi>()
    private val provider = mockk<ReferenceCodeProvider>()
    private val viewModel = ReferenceCodeViewModel(api, provider)

    @Test
    fun `test state() when provider has a value`() {
        every { provider.get() } returns ReferenceCode("some-code-100")

        val state = viewModel.state()

        assertThat(state.value).isEqualTo(Loaded(ReferenceCode("some-code-100")))
    }

    @Test
    fun `test state() when provider has no value and fetch successful`() {
        val deferred = Deferred<ReferenceCode>()

        every { provider.get() } returns null
        every { provider.set(any()) } returns Unit
        every { api.generate() } returns deferred.promise

        val state = viewModel.state()

        assertThat(state.value).isEqualTo(Loading)

        deferred.resolve(ReferenceCode("some-other-code-201"))

        verify { provider.set(ReferenceCode("some-other-code-201")) }
        assertThat(state.value).isEqualTo(Loaded(ReferenceCode("some-other-code-201")))
    }

    @Test
    fun `test state() when provider has no value and fetch fails`() {
        val deferred = Deferred<ReferenceCode>()

        every { provider.get() } returns null
        every { api.generate() } returns deferred.promise

        val state = viewModel.state()

        assertThat(state.value).isEqualTo(Loading)

        deferred.fail(RuntimeException("Oops"))

        assertThat(state.value).isEqualTo(ReferenceCodeViewModel.State.Error)
    }
}
