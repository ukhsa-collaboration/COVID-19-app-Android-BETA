/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import com.android.volley.ClientError

sealed class RegistrationResult {
    object Success : RegistrationResult()
    data class Failure(val exception: Exception) : RegistrationResult()
    class ActivationCodeNotValidFailure(e: ClientError) : RegistrationResult()
    object AlreadyRegistered : RegistrationResult()
    object WaitingForActivationCode : RegistrationResult()
}
