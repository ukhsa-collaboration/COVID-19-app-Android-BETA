/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import androidx.work.ListenableWorker.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.Promise

@ExperimentalCoroutinesApi
class ReferenceCodeWorkTest {

    private val api = mockk<ReferenceCodeApi>()
    private val provider = mockk<ReferenceCodeProvider>()
    private val work = ReferenceCodeWork(api, provider)

    @Test
    fun `doWork - on success`() = runBlockingTest {
        val deferred = Promise.Deferred<ReferenceCode>()
        val refCode = ReferenceCode("REF #1001")
        deferred.resolve(refCode)

        every { provider.get() } returns null
        every { api.generate() } returns deferred.promise
        every { provider.set(any()) } returns Unit

        val result = work.doWork()

        verifyAll {
            provider.get()
            api.generate()
            provider.set(refCode)
        }

        assertThat(result).isEqualTo(Result.success())
    }

    @Test
    fun `doWork - when already fetched`() = runBlockingTest {
        val refCode = ReferenceCode("REF #1001")

        every { provider.get() } returns refCode

        val result = work.doWork()

        assertThat(result).isEqualTo(Result.success())
    }

    @Test
    fun `doWork - on failure`() = runBlockingTest {
        val deferred = Promise.Deferred<ReferenceCode>()
        deferred.fail(Exception("Oops"))

        every { provider.get() } returns null
        every { api.generate() } returns deferred.promise

        val result = work.doWork()

        verify(exactly = 0) { provider.set(any()) }

        assertThat(result).isEqualTo(Result.failure())
    }
}
