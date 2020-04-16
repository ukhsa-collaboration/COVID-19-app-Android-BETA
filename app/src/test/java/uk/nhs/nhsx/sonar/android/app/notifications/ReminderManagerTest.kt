package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.util.hideRegistrationNotFinishedNotification

class ReminderManagerTest {

    private val context = mockk<Context>(relaxUnitFun = true)
    private val alarmManager = mockk<AlarmManager>(relaxUnitFun = true)
    private val reminderTimeProvider = mockk<ReminderTimeProvider>(relaxed = true)
    private val sut = ReminderManager(context, reminderTimeProvider, alarmManager)

    @Test
    fun setupReminder() {
        sut.setupReminder()

        verify { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), any()) }
        verify { reminderTimeProvider.provideTime() }
    }

    @Test
    fun cancelReminderCancelsAlarm() {
        sut.cancelReminder()

        verify { alarmManager.cancel(any<PendingIntent>()) }
    }

    @Test
    fun hideReminderNotificationHidesNotification() {
        mockkStatic("uk.nhs.nhsx.sonar.android.app.util.NotificationHelperKt")
        every { hideRegistrationNotFinishedNotification(any()) } returns Unit

        sut.hideReminderNotification()

        verify(exactly = 1) { hideRegistrationNotFinishedNotification(any()) }
    }
}
