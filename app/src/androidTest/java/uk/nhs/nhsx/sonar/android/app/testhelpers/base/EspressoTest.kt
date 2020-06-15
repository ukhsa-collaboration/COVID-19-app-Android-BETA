package uk.nhs.nhsx.sonar.android.app.testhelpers.base

import android.app.Activity
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestData
import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper

@RunWith(AndroidJUnit4::class)
abstract class EspressoTest {

    protected val testData = TestData()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*AndroidLocationHelper.requiredLocationPermissions)

    protected lateinit var testAppContext: TestApplicationContext

    @Before
    fun setup() {
        testAppContext = TestApplicationContext()
    }

    @After
    fun teardown() {
        testAppContext.teardown()
    }

    protected fun userState() = testAppContext.component.getUserStateStorage().get()

    protected inline fun <reified T : Activity> startActivityWithState(state: UserState = DefaultState) {
        testAppContext.setFullValidUser(state)
        startTestActivity<T>()
    }

    protected inline fun <reified T : Activity> startTestActivity(config: Intent.() -> Unit = {}) {
        val intent = Intent(testAppContext.app, T::class.java)
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
            .apply(config)

        InstrumentationRegistry
            .getInstrumentation()
            .startActivitySync(intent)
    }
}
