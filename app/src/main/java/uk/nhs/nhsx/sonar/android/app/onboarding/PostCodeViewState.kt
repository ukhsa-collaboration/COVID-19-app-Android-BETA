package uk.nhs.nhsx.sonar.android.app.onboarding

sealed class PostCodeViewState {
    object Valid : PostCodeViewState()
    object Invalid : PostCodeViewState()
}
