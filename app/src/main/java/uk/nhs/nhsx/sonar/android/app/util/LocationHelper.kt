package uk.nhs.nhsx.sonar.android.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.location.LocationManagerCompat

val requiredLocationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )
} else {
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
}

fun Context.isLocationEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

fun Context.locationPermissionsGranted(): Boolean {
    return requiredLocationPermissions.all { permission ->
        packageManager.checkPermission(
            permission,
            packageName
        ) == PackageManager.PERMISSION_GRANTED
    }
}
