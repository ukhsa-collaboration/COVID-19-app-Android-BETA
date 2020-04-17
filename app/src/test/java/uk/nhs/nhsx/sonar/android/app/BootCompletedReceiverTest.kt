/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.notifications.ReminderManager

class BootCompletedReceiverTest {

    private val reminderManager = mockk<ReminderManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val intent = mockk<Intent>()
    private val applicationComponent = mockk<ApplicationComponent>(relaxed = true)
    private val sut = BootCompletedReceiver()

    @Before
    fun setUp() {
        every { context.appComponent } returns applicationComponent
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        sut.sonarIdProvider = mockk(relaxed = true)
        sut.reminderManager = reminderManager
    }

    @Test
    fun onReceive() {
        sut.onReceive(context, intent)

        verify(exactly = 1) { reminderManager.handleBootComplete() }
    }
}
