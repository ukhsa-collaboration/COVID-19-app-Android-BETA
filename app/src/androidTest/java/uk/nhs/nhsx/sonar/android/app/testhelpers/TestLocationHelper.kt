package uk.nhs.nhsx.sonar.android.app.testhelpers

import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper

class TestLocationHelper(private val realHelper: AndroidLocationHelper) : LocationHelper {

    var locationEnabled: Boolean? = null
    var locationPermissionsGranted: Boolean? = null

    fun reset() {
        locationEnabled = null
        locationPermissionsGranted = null
    }

    override val requiredLocationPermissions: Array<String> =
        realHelper.requiredLocationPermissions

    override val providerChangedIntentAction: String =
        "uk.nhs.nhsx.sonar.android.PROVIDERS_CHANGED"

    override fun isLocationEnabled(): Boolean =
        locationEnabled ?: realHelper.isLocationEnabled()

    override fun locationPermissionsGranted(): Boolean =
        locationPermissionsGranted ?: realHelper.locationPermissionsGranted()
}
