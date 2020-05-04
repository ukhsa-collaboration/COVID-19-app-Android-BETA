/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.os.Build
import androidx.core.location.LocationManagerCompat

interface LocationHelper {
    val requiredLocationPermissions: Array<String>
    val providerChangedIntentAction: String
    fun isLocationEnabled(): Boolean
    fun locationPermissionsGranted(): Boolean
}

class AndroidLocationHelper(appContext: Context) : LocationHelper {

    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val packageManager = appContext.packageManager
    private val packageName = appContext.packageName

    companion object {
        val requiredLocationPermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
    }

    override val providerChangedIntentAction: String
        get() = LocationManager.PROVIDERS_CHANGED_ACTION

    override val requiredLocationPermissions: Array<String>
        get() = AndroidLocationHelper.requiredLocationPermissions

    override fun isLocationEnabled(): Boolean =
        LocationManagerCompat.isLocationEnabled(locationManager)

    override fun locationPermissionsGranted(): Boolean =
        requiredLocationPermissions.all { permission ->
            packageManager.checkPermission(permission, packageName) == PERMISSION_GRANTED
        }
}
