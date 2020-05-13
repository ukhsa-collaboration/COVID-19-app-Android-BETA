/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
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
    private val viewModel = ReferenceCodeViewModel(api)

    @Test
    fun `test state() when fetch successful`() {
        val deferred = Deferred<ReferenceCode>()

        every { api.generate() } returns deferred.promise

        val state = viewModel.state()

        assertThat(state.value).isEqualTo(Loading)

        deferred.resolve(ReferenceCode("some-other-code-201"))

        assertThat(state.value).isEqualTo(Loaded(ReferenceCode("some-other-code-201")))
    }

    @Test
    fun `test state() when and fetch fails`() {
        val deferred = Deferred<ReferenceCode>()

        every { api.generate() } returns deferred.promise

        val state = viewModel.state()

        assertThat(state.value).isEqualTo(Loading)

        deferred.fail("Oops")

        assertThat(state.value).isEqualTo(ReferenceCodeViewModel.State.Error)
    }
}
