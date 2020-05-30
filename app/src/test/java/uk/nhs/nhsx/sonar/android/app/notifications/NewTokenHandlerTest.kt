/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider

class NewTokenHandlerTest {
    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val tokenRefreshWorkScheduler = mockk<TokenRefreshWorkScheduler>(relaxUnitFun = true)

    private val handler = NewTokenHandler(
        sonarIdProvider,
        tokenRefreshWorkScheduler
    )

    @Test
    fun `test handleNewToken - when we have a sonar id`() {
        every { sonarIdProvider.hasProperSonarId() } returns true
        every { sonarIdProvider.get() } returns "sonar-id-200"

        handler.handle("some-token #1")

        verify { tokenRefreshWorkScheduler.schedule("sonar-id-200", "some-token #1") }
    }

    @Test
    fun `test handleNewToken - when we don't have a sonar id`() {
        every { sonarIdProvider.hasProperSonarId() } returns false

        handler.handle("some-token #1")

        verify { tokenRefreshWorkScheduler wasNot Called }
    }
}
