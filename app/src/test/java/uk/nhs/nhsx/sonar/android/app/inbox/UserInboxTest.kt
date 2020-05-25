/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.inbox

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test

class UserInboxTest {
    private val testInfoProvider = mockk<TestInfoProvider>()
    private val recoveryProvider = mockk<RecoveryProvider>()

    private val userInbox = UserInbox(testInfoProvider, recoveryProvider)

    @Test
    fun `adds new test result`() {
        every { testInfoProvider.set(any()) } returns Unit

        val testInfo = TestInfo(TestResult.INVALID, DateTime.now())

        userInbox.addTestResult(testInfo)

        verify {
            testInfoProvider.set(testInfo)
        }
    }

    @Test
    fun `checks for existing test result`() {
        every { testInfoProvider.has() } returns true

        assertThat(userInbox.hasTestResult()).isTrue()
    }

    @Test
    fun `checks for non-existing test result`() {
        every { testInfoProvider.has() } returns false

        assertThat(userInbox.hasTestResult()).isFalse()
    }

    @Test
    fun `dismisses a test result`() {
        every { testInfoProvider.clear() } returns Unit

        assertThat(userInbox.dismissTestResult())

        verify {
            testInfoProvider.clear()
        }
    }

    @Test
    fun `adds new recovery message`() {
        every { recoveryProvider.set(any()) } returns Unit

        userInbox.addRecovery()

        verify {
            recoveryProvider.set("Recovery")
        }
    }

    @Test
    fun `checks for existing recovery message`() {
        every { recoveryProvider.has() } returns true

        assertThat(userInbox.hasRecovery()).isTrue()
    }

    @Test
    fun `checks for non-existing recovery message`() {
        every { recoveryProvider.has() } returns false

        assertThat(userInbox.hasRecovery()).isFalse()
    }

    @Test
    fun `dismisses a recovery message`() {
        every { recoveryProvider.clear() } returns Unit

        assertThat(userInbox.dismissRecovery())

        verify {
            recoveryProvider.clear()
        }
    }
}
