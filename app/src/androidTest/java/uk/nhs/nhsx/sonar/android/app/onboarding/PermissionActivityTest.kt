package uk.nhs.nhsx.sonar.android.app.onboarding

import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.edgecases.EdgeCaseRobot
import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.StatusRobot
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText
import kotlin.test.fail

class PermissionActivityTest(private val testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val permissionRobot = PermissionRobot()
    private val statusRobot = StatusRobot()
    private val edgeCaseRobot = EdgeCaseRobot()

    private fun startActivity() {
        testAppContext.setValidPostcode()
        app.startTestActivity<PermissionActivity>()
    }

    fun testUnsupportedDevice() {
        testAppContext.simulateUnsupportedDevice()

        startActivity()
        permissionRobot.clickContinue()

        edgeCaseRobot.checkTitle(R.string.device_not_supported_title)
    }

    fun testEnableBluetooth() {
        testAppContext.ensureBluetoothDisabled()

        startActivity()
        permissionRobot.clickContinue()

        testAppContext.device.apply {
            wait(Until.hasObject(By.textContains("wants to turn on Bluetooth")), 500)

            val button = sequenceOf("Allow", "Yes", "Ok", "Accept")
                .mapNotNull(::findButton)
                .firstOrNull()
                ?: fail("Looks like we could not find the acceptance button for bluetooth.")

            button.click()
        }

        testAppContext.verifyBluetoothIsEnabled()
        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    fun testGrantLocationPermission() {
        testAppContext.revokeLocationPermission()

        startActivity()
        permissionRobot.clickContinue()

        if (Build.VERSION.SDK_INT >= 29) {
            checkViewHasText(R.id.edgeCaseTitle, R.string.grant_location_permission_title)
        } else {
            checkViewHasText(
                R.id.edgeCaseTitle,
                R.string.grant_location_permission_title_pre_10
            )
        }

        onView(withId(R.id.takeActionButton)).perform(click())

        // ensure we leave the screen before moving on
        testAppContext.waitUntilCannotFindText(R.string.grant_location_permission_title)
        testAppContext.waitUntilCannotFindText(R.string.grant_location_permission_title_pre_10)

        // moving on...
        testAppContext.grantLocationPermission()
        testAppContext.device.pressBack()

        permissionRobot.checkActivityIsDisplayed()
        permissionRobot.clickContinue()
        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    fun testEnableLocationAccess() {
        testAppContext.disableLocationAccess()

        startActivity()
        permissionRobot.clickContinue()

        onView(withId(R.id.edgeCaseTitle))
            .check(ViewAssertions.matches(withText(R.string.enable_location_service_title)))

        onView(withId(R.id.takeActionButton)).perform(click())
        testAppContext.enableLocationAccess()
        testAppContext.device.pressBack()

        permissionRobot.checkActivityIsDisplayed()
        permissionRobot.clickContinue()
        statusRobot.checkActivityIsDisplayed(DefaultState::class)
    }

    private fun findButton(text: String): UiObject2? =
        testAppContext.device.let {
            it.findObject(By.text(text))
                ?: it.findObject(By.text(text.toLowerCase()))
                ?: it.findObject(By.text(text.toUpperCase()))
        }
}
