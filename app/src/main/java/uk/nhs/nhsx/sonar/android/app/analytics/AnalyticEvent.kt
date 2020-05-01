/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.analytics

const val REGISTRATION_SUCCEEDED = "Registration succeeded"

fun registrationSucceeded(): AnalyticEvent = AnalyticEvent(REGISTRATION_SUCCEEDED)

fun registrationSendTokenCallFailed(statusCode: Int?): AnalyticEvent = AnalyticEvent(
    "Registration failed",
    mapOf(
        "Reason" to "Registration call failed",
        "Status code" to statusCode.toString()
    )
)

fun registrationFailedWaitingForFCMToken(): AnalyticEvent = AnalyticEvent(
    "Registration failed",
    mapOf(
        "Reason" to "No FCM token"
    )
)

fun registrationFailedWaitingForActivationNotification(): AnalyticEvent = AnalyticEvent(
    "Registration failed",
    mapOf(
        "Reason" to "Activation notification not received"
    )
)

fun registrationFailedActivationCallFailed(statusCode: Int?): AnalyticEvent = AnalyticEvent(
    "Registration failed",
    mapOf(
        "Reason" to "Activation call failed",
        "Status code" to statusCode.toString()
    )
)

fun partialPostcodeProvided(): AnalyticEvent = AnalyticEvent(
    "Partial postcode provided"
)

fun onboardingCompleted(): AnalyticEvent = AnalyticEvent(
    "Onboarding completed"
)

fun collectedContactEvents(yesterday: Int, all: Int): AnalyticEvent = AnalyticEvent(
    "Collected contact events",
    mapOf(
        "Yesterday" to yesterday.toString(),
        "All" to all.toString()
    )
)

data class AnalyticEvent(
    val name: String,
    val properties: Map<String, String>? = null
)
