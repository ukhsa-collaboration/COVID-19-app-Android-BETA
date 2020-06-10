package uk.nhs.nhsx.sonar.android.app

import androidx.test.runner.permission.PermissionRequester
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper

abstract class EspressoJunit5Test {

    protected val testAppContext = TestApplicationContext()

    @BeforeEach
    fun setup() {
        testAppContext.reset()

        grantPermissions()
    }

    @AfterEach
    fun cleanup() {
        testAppContext.shutdownMockServer()
    }

    private fun grantPermissions() {
        PermissionRequester().apply {
            addPermissions(*AndroidLocationHelper.requiredLocationPermissions)
            requestPermissions()
        }
    }
}
