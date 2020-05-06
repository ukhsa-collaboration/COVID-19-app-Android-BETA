/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

sealed class RegistrationResult {
    object Success : RegistrationResult()
    object WaitingForActivationCode : RegistrationResult()
    object Error : RegistrationResult()
}
