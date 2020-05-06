/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.analytics

interface SonarAnalytics {
    fun trackEvent(event: AnalyticEvent)
}
