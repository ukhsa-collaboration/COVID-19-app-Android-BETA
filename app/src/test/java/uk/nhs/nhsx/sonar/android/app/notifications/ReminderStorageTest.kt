package uk.nhs.nhsx.sonar.android.app.notifications

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.Reminder
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderStorage

class ReminderStorageTest {

    private val sharedPreferences = mockk<SharedPreferences>()
    private val context = mockk<Context> {
        every {
            getSharedPreferences("reminder_notification_storage", Context.MODE_PRIVATE)
        } returns sharedPreferences
    }

    private val storage = ReminderStorage(context)

    @Test
    fun `getPendingReminder - when there is a legacy reminder`() {
        every { sharedPreferences.all } returns mutableMapOf("reminder_notification" to 1L)

        val reminder = storage.getPendingReminder()

        assertThat(reminder).isEqualTo(
            Reminder(
                time = 1,
                type = Reminder.REMINDER_TYPE_CHECK_IN
            )
        )
    }

    @Test
    fun `getPendingReminder - when there is a pending reminder`() {
        every { sharedPreferences.all } returns
            mutableMapOf("reminder_notification" to """{"time": 1, "type": 3}""")

        val reminder = storage.getPendingReminder()

        assertThat(reminder).isEqualTo(
            Reminder(
                time = 1,
                type = 3
            )
        )
    }

    @Test
    fun `getPendingReminder - when there is no pending reminder`() {
        every { sharedPreferences.all } returns mutableMapOf<String, Any?>()

        val reminder = storage.getPendingReminder()

        assertThat(reminder).isNull()
    }

    @Test
    fun `setPendingReminder - sets the right reminder`() {
        val editor = mockk<SharedPreferences.Editor>()
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } returns Unit
        every { sharedPreferences.edit() } returns editor

        val reminder = Reminder(3, 2)
        storage.setPendingReminder(reminder)

        verify {
            sharedPreferences.edit()
            editor.putString("reminder_notification", """{"type":2,"time":3}""")
            editor.apply()
        }
    }
}
