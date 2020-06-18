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
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.Reminder
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.Reminder.Companion.REMINDER_TYPE
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.Reminder.Companion.REMINDER_TYPE_CHECK_IN
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.Reminder.Companion.REMINDER_TYPE_EXPIRED_EXPOSED
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderBroadcastFactory
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderStorage
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderScheduler

class ReminderSchedulerTest {

    private val reminderStorage = mockk<ReminderStorage>()
    private val alarmManager = mockk<AlarmManager>()
    private val expiredExposedReminderNotification = mockk<ExpiredExposedReminderNotification>()
    private val checkInReminderNotification = mockk<CheckInReminderNotification>()
    private val reminderBroadcastFactory = mockk<ReminderBroadcastFactory>()
    private val reminderScheduler =
        ReminderScheduler(
            alarmManager,
            reminderStorage,
            checkInReminderNotification,
            expiredExposedReminderNotification,
            reminderBroadcastFactory
        )

    @Test
    fun `scheduleExpiredExposedReminder - schedules correct reminder`() {
        val broadcast = mockk<PendingIntent>()

        every { reminderBroadcastFactory.create(any()) } returns broadcast
        every { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) } returns Unit
        every { reminderStorage.setPendingReminder(any()) } returns Unit

        val time = DateTime.parse("2020-04-28T15:20:00Z")

        reminderScheduler.scheduleExpiredExposedReminder(time)

        verifyAll {
            reminderBroadcastFactory.create(REMINDER_TYPE_EXPIRED_EXPOSED)
            alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, time.millis, broadcast)
            reminderStorage.setPendingReminder(
                Reminder(
                    time = time.millis,
                    type = REMINDER_TYPE_EXPIRED_EXPOSED
                )
            )
        }
    }

    @Test
    fun `scheduleCheckInReminder - schedules correct reminder`() {
        val broadcast = mockk<PendingIntent>()

        every { reminderBroadcastFactory.create(any()) } returns broadcast
        every { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) } returns Unit
        every { reminderStorage.setPendingReminder(any()) } returns Unit

        val time = DateTime.parse("2020-04-28T15:20:00Z")

        reminderScheduler.scheduleCheckInReminder(time)

        verifyAll {
            reminderBroadcastFactory.create(REMINDER_TYPE_CHECK_IN)
            alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, time.millis, broadcast)
            reminderStorage.setPendingReminder(
                Reminder(
                    time = time.millis,
                    type = REMINDER_TYPE_CHECK_IN
                )
            )
        }
    }

    @Test
    fun `reschedulePendingReminder - when there is a pending reminder`() {
        val broadcast = mockk<PendingIntent>()

        val time = DateTime.parse("2020-04-28T15:20:00Z")

        every { reminderBroadcastFactory.create(any()) } returns broadcast
        every { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) } returns Unit
        every { reminderStorage.getPendingReminder() } returns
            Reminder(
                time = time.millis,
                type = REMINDER_TYPE_CHECK_IN
            )

        reminderScheduler.reschedulePendingReminder()

        verifyAll {
            reminderBroadcastFactory.create(REMINDER_TYPE_CHECK_IN)
            alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, time.millis, broadcast)
        }
    }

    @Test
    fun `reschedulePendingReminder - when there is no pending reminder`() {
        every { reminderStorage.getPendingReminder() } returns null

        reminderScheduler.reschedulePendingReminder()

        verifyAll {
            alarmManager wasNot Called
            reminderBroadcastFactory wasNot Called
        }
    }

    @Test
    fun cancelReminders() {
        val checkInIntent = mockk<PendingIntent> {
            every { cancel() } returns Unit
        }
        val expiredExposedIntent = mockk<PendingIntent> {
            every { cancel() } returns Unit
        }

        every { reminderBroadcastFactory.create(REMINDER_TYPE_CHECK_IN) } returns checkInIntent
        every { reminderBroadcastFactory.create(REMINDER_TYPE_EXPIRED_EXPOSED) } returns expiredExposedIntent
        every { reminderStorage.clear() } returns Unit

        reminderScheduler.cancelReminders()

        verifyAll {
            checkInIntent.cancel()
            expiredExposedIntent.cancel()
            reminderStorage.clear()
        }
    }

    @Test
    fun `handleReminderBroadcast - with check-in reminder intent`() {
        every { checkInReminderNotification.show() } returns Unit
        every { reminderStorage.clear() } returns Unit

        reminderScheduler.handleReminderBroadcast(TestIntent(REMINDER_TYPE_CHECK_IN))

        verify {
            checkInReminderNotification.show()
            reminderStorage.clear()
        }
    }

    @Test
    fun `handleReminderBroadcast - with expired exposed reminder intent`() {
        every { expiredExposedReminderNotification.show() } returns Unit
        every { reminderStorage.clear() } returns Unit

        reminderScheduler.handleReminderBroadcast(TestIntent(REMINDER_TYPE_EXPIRED_EXPOSED))

        verify {
            expiredExposedReminderNotification.show()
            reminderStorage.clear()
        }
    }

    @Test
    fun `handleReminderBroadcast - with any other intent`() {
        reminderScheduler.handleReminderBroadcast(TestIntent(null))

        verify {
            checkInReminderNotification wasNot Called
            reminderStorage wasNot Called
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
