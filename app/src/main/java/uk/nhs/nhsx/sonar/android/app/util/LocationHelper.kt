package uk.nhs.nhsx.sonar.android.app.util

import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat

fun Context.isLocationEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}
