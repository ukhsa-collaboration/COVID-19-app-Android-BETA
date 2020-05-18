/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.analytics

class AppCenterAnalytics : SonarAnalytics {
    override fun trackEvent(event: AnalyticEvent) {
        // Removed AppCenter Analytics: https://www.pivotaltracker.com/story/show/172855736
    }
}
