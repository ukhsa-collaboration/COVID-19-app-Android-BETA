/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

sealed class RegistrationResult {
    object Success : RegistrationResult()
    data class Failure(val exception: Exception) : RegistrationResult()
    object AlreadyRegistered : RegistrationResult()
}
