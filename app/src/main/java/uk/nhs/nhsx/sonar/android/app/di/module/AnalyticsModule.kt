/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.analytics.AppCenterAnalytics
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics

@Module
class AnalyticsModule {
    @Provides
    fun provideAnalytics(): SonarAnalytics = AppCenterAnalytics()
}
