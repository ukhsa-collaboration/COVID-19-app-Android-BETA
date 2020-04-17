/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import java.util.Calendar
import javax.inject.Inject
import kotlin.random.Random

class FixedReminderTimeProvider @Inject constructor(
    private val lastReminderNotificationDateProvider: LastReminderNotificationDateProvider,
    private val currentDateTimeProvider: () -> Calendar = { Calendar.getInstance() }
) : ReminderTimeProvider {
    override fun provideNextReminderTime(): Long {
        val currentDateTime = currentDateTimeProvider()
        val lastReminderNotificationDate =
            lastReminderNotificationDateProvider.getLastReminderNotificationDate()
        if (isSameDay(currentDateTime, lastReminderNotificationDate)) {
            return dailyReminderTime(Day.TOMORROW)
        }
        val currentHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        if (currentHour < 9) {
            return dailyReminderTime(Day.TODAY)
        }

        return fiveMinutesReminderTime()
    }

    override fun setLastReminderNotificationTime(time: Calendar) {
        lastReminderNotificationDateProvider.setLastReminderNotificationDate(time)
    }

    private fun dailyReminderTime(day: Day): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDateTimeProvider().timeInMillis
        calendar.add(Calendar.DATE, day.numberOfDays)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, Random.nextInt(0, 60))
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }

    private fun fiveMinutesReminderTime(): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDateTimeProvider().timeInMillis
        calendar.add(Calendar.MINUTE, 5)
        return calendar.timeInMillis
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    enum class Day(val numberOfDays: Int) {
        TODAY(0), TOMORROW(1)
    }
}
