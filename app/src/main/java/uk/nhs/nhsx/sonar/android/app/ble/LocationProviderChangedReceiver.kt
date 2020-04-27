package uk.nhs.nhsx.sonar.android.app.ble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper

class LocationProviderChangedReceiver(private val locationHelper: LocationHelper) : BroadcastReceiver() {

    private var isGpsEnabled: Boolean = false
    private var isNetworkEnabled: Boolean = false

    private val subject = BehaviorSubject.create<Boolean>()

    fun getLocationStatus(): Observable<Boolean> =
        subject.distinctUntilChanged()

    fun onCreate() = checkStatus()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == locationHelper.providerChangedIntentAction)
            checkStatus()
    }

    private fun checkStatus() {
        isGpsEnabled = locationHelper.isProviderEnabled(GPS_PROVIDER)
        isNetworkEnabled = locationHelper.isProviderEnabled(NETWORK_PROVIDER)

        Timber.i("Location Providers changed, is GPS Enabled: $isGpsEnabled is network enabled = $isNetworkEnabled")

        subject.onNext(isGpsEnabled || isNetworkEnabled)
    }
}
