/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager

class ActivationCodeMessageHandlerTest {

    private val activationCodeProvider = mockk<ActivationCodeProvider>(relaxUnitFun = true)
    private val registrationManager = mockk<RegistrationManager>(relaxUnitFun = true)

    private val handler = ActivationCodeMessageHandler(
        activationCodeProvider,
        registrationManager
    )

    @Test
    fun `handle activation code message`() {
        val message = ActivationCodeMessage(
            handler,
            acknowledgmentUrl = "::a url::",
            code = "::a code::"
        )

        handler.handle(message)

        verify {
            activationCodeProvider.set("::a code::")
            registrationManager.register()
        }
    }
}
