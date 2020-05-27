/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.content.Intent
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders.Companion.REMINDER_TYPE
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders.Companion.REQUEST_CODE_CHECK_IN_REMINDER

class RemindersTest {

    private val checkInReminder = mockk<CheckInReminder>()
    private val alarmManager = mockk<AlarmManager>()
    private val checkInReminderNotification = mockk<CheckInReminderNotification>()
    private val reminderBroadcastFactory = mockk<ReminderBroadcastFactory>()
    private val reminders = Reminders(
        alarmManager,
        checkInReminder,
        checkInReminderNotification,
        reminderBroadcastFactory
    )

    @Test
    fun scheduleCheckInReminder() {
        val broadcast = mockk<PendingIntent>()

        every { reminderBroadcastFactory.create(any()) } returns broadcast
        every { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) } returns Unit
        every { checkInReminder.setPendingReminder(any()) } returns Unit

        val time = DateTime.parse("2020-04-28T15:20:00Z")

        reminders.scheduleCheckInReminder(time)

        verifyAll {
            reminderBroadcastFactory.create(REQUEST_CODE_CHECK_IN_REMINDER)
            alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, time.millis, broadcast)
            checkInReminder.setPendingReminder(time.millis)
        }
    }

    @Test
    fun `rescheduleCheckInReminder - when there is a pending reminder`() {
        val broadcast = mockk<PendingIntent>()

        val time = DateTime.parse("2020-04-28T15:20:00Z")

        every { reminderBroadcastFactory.create(any()) } returns broadcast
        every { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) } returns Unit
        every { checkInReminder.getPendingReminder() } returns time.millis

        reminders.reschedulePendingCheckInReminder()

        verifyAll {
            reminderBroadcastFactory.create(REQUEST_CODE_CHECK_IN_REMINDER)
            alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, time.millis, broadcast)
        }
    }

    @Test
    fun `rescheduleCheckInReminder - when there is no pending reminder`() {
        every { checkInReminder.getPendingReminder() } returns null

        reminders.reschedulePendingCheckInReminder()

        verifyAll {
            alarmManager wasNot Called
            reminderBroadcastFactory wasNot Called
        }
    }

    @Test
    fun `handleReminderBroadcast - with check in reminder intent`() {
        every { checkInReminderNotification.show() } returns Unit
        every { checkInReminder.clear() } returns Unit

        reminders.handleReminderBroadcast(TestIntent(REQUEST_CODE_CHECK_IN_REMINDER))

        verify {
            checkInReminderNotification.show()
            checkInReminder.clear()
        }
    }

    @Test
    fun `handleReminderBroadcast - with any other intent`() {
        reminders.handleReminderBroadcast(TestIntent(null))

        verify {
            checkInReminderNotification wasNot Called
            checkInReminder wasNot Called
        }
    }

    class TestIntent(private val reminderType: Int?) : Intent() {

        override fun getIntExtra(name: String, defaultValue: Int): Int =
            when {
                reminderType == null -> defaultValue
                name == REMINDER_TYPE -> reminderType
                else -> super.getIntExtra(name, defaultValue)
            }
    }
}
