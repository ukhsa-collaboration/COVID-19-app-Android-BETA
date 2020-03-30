package com.example.colocate.registration

sealed class RegistrationResult {
    object Success : RegistrationResult()
    data class Failure(val exception: Exception) : RegistrationResult()
    object AlreadyRegistered : RegistrationResult()
}
