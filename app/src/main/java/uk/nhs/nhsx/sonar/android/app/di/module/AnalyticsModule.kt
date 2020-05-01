package uk.nhs.nhsx.sonar.android.app.di.module

import com.microsoft.appcenter.analytics.Analytics
import dagger.Module
import dagger.Provides

@Module
class AnalyticsModule(
    private val analytics: Analytics
) {
    @Provides
    fun provideAnalytics(): Analytics = analytics
}
