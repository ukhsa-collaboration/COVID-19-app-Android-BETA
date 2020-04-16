package uk.nhs.nhsx.sonar.android.app.notifications

interface ReminderTimeProvider {
    fun provideTime(): Long
}