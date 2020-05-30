/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager
import javax.inject.Inject

class ActivationCodeMessageHandler @Inject constructor(
    private val activationCodeProvider: ActivationCodeProvider,
    private val registrationManager: RegistrationManager
) {

    fun handle(message: ActivationCodeMessage) {
        activationCodeProvider.set(message.code)
        registrationManager.register()
    }
}
