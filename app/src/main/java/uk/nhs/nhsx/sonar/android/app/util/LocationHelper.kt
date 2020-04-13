package uk.nhs.nhsx.sonar.android.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun hasLocationPermission(context: Context): Boolean {
    return checkCoarseLocationPermissions(context) == PackageManager.PERMISSION_GRANTED &&
        checkFineLocationPermissions(context) == PackageManager.PERMISSION_GRANTED
}

fun checkFineLocationPermissions(context: Context) = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_FINE_LOCATION
)

fun checkCoarseLocationPermissions(context: Context) = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_COARSE_LOCATION
)
