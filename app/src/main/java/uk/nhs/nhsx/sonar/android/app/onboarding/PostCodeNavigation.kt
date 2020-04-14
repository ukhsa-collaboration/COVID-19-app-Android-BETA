package uk.nhs.nhsx.sonar.android.app.onboarding

sealed class PostCodeNavigation {
    object Permissions : PostCodeNavigation()
}
