package uk.nhs.nhsx.sonar.android.app.notifications

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import kotlin.test.assertEquals

class FixedReminderTimeProviderTest {

    private val lastReminderNotificationDateProvider = mockk<LastReminderNotificationDateProvider>()
    private val currentDateTimeProvider = mockk<() -> Calendar>()
    private val sut =
        FixedReminderTimeProvider(lastReminderNotificationDateProvider, currentDateTimeProvider)

    @Before
    fun setUp() {
        every { lastReminderNotificationDateProvider.getLastReminderNotificationDate() } returns may5(
            9
        )
    }

    @Test
    fun ifAlreadyFiredTodayReturnReminderTimeForTomorrow() {
        every { currentDateTimeProvider.invoke() } returns may5(12)

        val nextReminderDateTime =
            Calendar.getInstance().apply { timeInMillis = sut.provideNextReminderTime() }
        assertEquals(6, nextReminderDateTime.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, nextReminderDateTime.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun ifNotFiredTodayAndCurrentTimeIsBefore9AmReturnReminderTimeForToday() {
        every { currentDateTimeProvider.invoke() } returns may6(8)

        val nextReminderDateTime =
            Calendar.getInstance().apply { timeInMillis = sut.provideNextReminderTime() }
        assertEquals(6, nextReminderDateTime.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, nextReminderDateTime.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun ifNotFiredTodayAndCurrentTimeIsAfter9AmReturnReminderTimeInFiveMinutes() {
        every { currentDateTimeProvider.invoke() } returns may6(13)

        val nextReminderDateTime =
            Calendar.getInstance().apply { timeInMillis = sut.provideNextReminderTime() }
        assertEquals(6, nextReminderDateTime.get(Calendar.DAY_OF_MONTH))
        assertEquals(13, nextReminderDateTime.get(Calendar.HOUR_OF_DAY))
        assertEquals(5, nextReminderDateTime.get(Calendar.MINUTE))
    }

    private fun may5(hourOfDay: Int): Calendar {
        return Calendar.getInstance()
            .apply {
                set(2020, 4, 5, hourOfDay, 0, 0)
            }
    }

    private fun may6(hourOfDay: Int): Calendar {
        return Calendar.getInstance()
            .apply {
                set(2020, 4, 6, hourOfDay, 0, 0)
            }
    }
}
