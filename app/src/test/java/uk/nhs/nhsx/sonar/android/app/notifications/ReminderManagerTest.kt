/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.util.hideRegistrationNotFinishedNotification
import uk.nhs.nhsx.sonar.android.app.util.showRegistrationReminderNotification

class ReminderManagerTest {

    private val context = mockk<Context>(relaxUnitFun = true)
    private val alarmManager = mockk<AlarmManager>(relaxUnitFun = true)
    private val reminderTimeProvider = mockk<ReminderTimeProvider>(relaxed = true)
    private val sonarIdProvider = mockk<SonarIdProvider>(relaxed = true)
    private val sut =
        spyk(ReminderManager(context, reminderTimeProvider, alarmManager, sonarIdProvider))

    @Before
    fun setUp() {
        mockkStatic("uk.nhs.nhsx.sonar.android.app.util.NotificationHelperKt")
        every { showRegistrationReminderNotification(any()) } returns Unit
        every { hideRegistrationNotFinishedNotification(any()) } returns Unit
    }

    @Test
    fun setupReminder() {
        sut.scheduleReminder()
        verify { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), any()) }
        verify { reminderTimeProvider.provideNextReminderTime() }
    }

    @Test
    fun cancelReminderCancelsAlarm() {
        sut.cancelReminder()

        verify { alarmManager.cancel(any<PendingIntent>()) }
    }

    @Test
    fun hideReminderNotificationHidesNotification() {
        sut.hideReminderNotification()

        verify(exactly = 1) { hideRegistrationNotFinishedNotification(any()) }
    }

    @Test
    fun showReminderNotificationShowsNotification() {
        sut.showNotification()

        verify(exactly = 1) { showRegistrationReminderNotification(any()) }
    }

    @Test
    fun showReminderNotificationSetsLastReminderNotificationTime() {
        sut.showNotification()

        verify(exactly = 1) { reminderTimeProvider.setLastReminderNotificationTime(any()) }
    }

    @Test
    fun handleBroadcastShowsNotificationAndSchedulesReminderIfUserNotRegistered() {
        every { sonarIdProvider.hasProperSonarId() } returns false

        sut.handleReminderBroadcast()

        verify(exactly = 1) {
            sut.showNotification()
            sut.scheduleReminder()
        }
    }

    @Test
    fun handleBroadcastShowsNotificationAndSchedulesReminderIfUserIsRegistered() {
        every { sonarIdProvider.hasProperSonarId() } returns true

        sut.handleReminderBroadcast()

        verify(exactly = 0) {
            sut.showNotification()
            sut.scheduleReminder()
        }
    }

    @Test
    fun handleBootCompleteSchedulesReminderIfUserNotRegistered() {
        every { sonarIdProvider.hasProperSonarId() } returns false

        sut.handleBootComplete()

        verify(exactly = 1) {
            sut.scheduleReminder()
        }
    }

    @Test
    fun handleBootCompleteDoesNotSchedulesReminderIfUserIsRegistered() {
        every { sonarIdProvider.hasProperSonarId() } returns true

        sut.handleBootComplete()

        verify(exactly = 0) {
            sut.scheduleReminder()
        }
    }
}
