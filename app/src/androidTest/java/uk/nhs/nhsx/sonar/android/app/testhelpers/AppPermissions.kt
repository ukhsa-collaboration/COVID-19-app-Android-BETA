package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.content.Intent
import uk.nhs.nhsx.sonar.android.app.SonarApplication
import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper
import uk.nhs.nhsx.sonar.android.app.util.TestNotificationManagerHelper

class AppPermissions(
    private val app: SonarApplication,
    private val testNotificationManagerHelper: TestNotificationManagerHelper
) {

    val testLocationHelper =
        TestLocationHelper(
            AndroidLocationHelper(app)
        )

    fun disableLocationAccess() {
        testLocationHelper.locationEnabled = false
        app.sendBroadcast(Intent(testLocationHelper.providerChangedIntentAction))
    }

    fun enableLocationAccess() {
        testLocationHelper.locationEnabled = true
        app.sendBroadcast(Intent(testLocationHelper.providerChangedIntentAction))
    }

    fun revokeLocationPermission() {
        testLocationHelper.locationPermissionsGranted = false
    }

    fun grantLocationPermission() {
        testLocationHelper.locationPermissionsGranted = true
    }

    fun revokeNotificationsPermission() {
        testNotificationManagerHelper.notificationEnabled = false
    }

    fun grantNotificationsPermission() {
        testNotificationManagerHelper.notificationEnabled = true
    }
}
