package uk.nhs.nhsx.sonar.android.app.persistence

interface OnboardingStatusProvider {
    fun isOnboardingFinished(): Boolean
    fun setOnboardingFinished(finished: Boolean)
}
