package uk.nhs.nhsx.sonar.android.app.ble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper

class LocationProviderChangedReceiver(private val locationHelper: LocationHelper) : BroadcastReceiver() {

    private val subject = BehaviorSubject.create<Boolean>()

    fun getLocationStatus(): Observable<Boolean> =
        subject.distinctUntilChanged()

    fun onCreate() = checkStatus()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == locationHelper.providerChangedIntentAction)
            checkStatus()
    }

    private fun checkStatus() =
        subject.onNext(locationHelper.isLocationEnabled())
}
