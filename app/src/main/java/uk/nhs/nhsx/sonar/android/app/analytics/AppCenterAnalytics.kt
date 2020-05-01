package uk.nhs.nhsx.sonar.android.app.analytics

import com.microsoft.appcenter.analytics.Analytics

class AppCenterAnalytics : SonarAnalytics {
    override fun trackEvent(event: AnalyticEvent) {
        Analytics.trackEvent(event.name, event.properties)
    }
}
