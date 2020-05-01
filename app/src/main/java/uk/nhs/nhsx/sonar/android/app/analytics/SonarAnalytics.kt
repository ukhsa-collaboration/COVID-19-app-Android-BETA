package uk.nhs.nhsx.sonar.android.app.analytics

interface SonarAnalytics {
    fun trackEvent(event: AnalyticEvent)
}
