/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.content.Intent
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage

class PackageReplacedReceiverTest {
    private val stateStorage = mockk<UserStateStorage>()
    private val reminders = mockk<Reminders>()
    private val context = mockk<Context>()

    private val receiver = PackageReplacedReceiver().also {
        it.userStateStorage = stateStorage
        it.reminders = reminders
    }

    @Before
    fun setUp() {
        val appComponent = mockk<ApplicationComponent>()
        every { context.appComponent } returns appComponent
        every { appComponent.inject(receiver) } returns Unit
    }

    @Test
    fun `onReceive - with unknown intent action`() {
        val intent = TestIntent("SOME_OTHER_ACTION")

        receiver.onReceive(context, intent)

        verifyAll {
            stateStorage wasNot Called
        }
    }

    @Test
    fun `onReceive - with package-replaced intent action`() {
        val userState = mockk<UserState>()
        every { userState.scheduleCheckInReminder(reminders) } returns Unit

        every { stateStorage.get() } returns userState

        receiver.onReceive(context, TestIntent(Intent.ACTION_MY_PACKAGE_REPLACED))

        verifyAll {
            userState.scheduleCheckInReminder(reminders)
        }
    }

    class TestIntent(private val actionName: String) : Intent() {
        override fun getAction() = actionName
    }
}
