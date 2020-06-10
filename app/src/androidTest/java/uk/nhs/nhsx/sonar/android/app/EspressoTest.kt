package uk.nhs.nhsx.sonar.android.app

import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper

abstract class EspressoTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*AndroidLocationHelper.requiredLocationPermissions)

    protected lateinit var testAppContext: TestApplicationContext

    @Before
    fun setup() {
        testAppContext = TestApplicationContext()
        testAppContext.reset()
    }

    @After
    fun teardown() {
        testAppContext.shutdownMockServer()
    }
}
