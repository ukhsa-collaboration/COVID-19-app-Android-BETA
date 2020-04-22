package uk.nhs.nhsx.sonar.android.app.ble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.location.LocationManager.PROVIDERS_CHANGED_ACTION
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

class LocationProviderChangedReceiver(private val locationManager: LocationManager) : BroadcastReceiver() {

    private var isGpsEnabled: Boolean = false
    private var isNetworkEnabled: Boolean = false

    private val subject = BehaviorSubject.create<Boolean>()

    fun getLocationStatus(): Observable<Boolean> =
        subject.distinctUntilChanged()

    fun onCreate() = checkStatus()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PROVIDERS_CHANGED_ACTION)
            checkStatus()
    }

    private fun checkStatus() {
        isGpsEnabled = locationManager.isProviderEnabled(GPS_PROVIDER)
        isNetworkEnabled = locationManager.isProviderEnabled(NETWORK_PROVIDER)

        Timber.i("Location Providers changed, is GPS Enabled: $isGpsEnabled is network enabled = $isNetworkEnabled")

        subject.onNext(isGpsEnabled || isNetworkEnabled)
    }
}
