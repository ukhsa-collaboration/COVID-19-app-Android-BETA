package uk.nhs.nhsx.sonar.android.app.notifications

import java.util.Calendar
import kotlin.random.Random

class FixedReminderTimeProvider : ReminderTimeProvider {
    override fun provideTime(): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, Random.nextInt(0, 60))
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }
}