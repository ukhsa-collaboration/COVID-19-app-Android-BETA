/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter.STATE_OFF
import android.os.Bundle
import android.view.View
import io.reactivex.disposables.Disposable
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothStateBroadcastReceiver
import uk.nhs.nhsx.sonar.android.app.ble.LocationProviderChangedReceiver
import uk.nhs.nhsx.sonar.android.app.debug.TesterActivity
import uk.nhs.nhsx.sonar.android.app.edgecases.ReAllowGrantLocationPermissionActivity
import uk.nhs.nhsx.sonar.android.app.edgecases.ReEnableBluetoothActivity
import uk.nhs.nhsx.sonar.android.app.edgecases.ReEnableLocationActivity
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper
import uk.nhs.nhsx.sonar.android.app.util.isBluetoothDisabled
import javax.inject.Inject

abstract class BaseActivity : ColorInversionAwareActivity() {

    private var locationSubscription: Disposable? = null

    @Inject
    lateinit var locationHelper: LocationHelper

    private lateinit var locationProviderChangedReceiver: LocationProviderChangedReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        locationProviderChangedReceiver = LocationProviderChangedReceiver(locationHelper)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        when (BuildConfig.BUILD_TYPE) {
            "internal", "debug" -> {
                findViewById<View>(R.id.logo)
                    ?.setOnClickListener { TesterActivity.start(this) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        listenBluetoothChange()
        checkLocationPermission()
        listenLocationChange()
    }

    private fun listenBluetoothChange() {
        if (isBluetoothDisabled()) {
            ReEnableBluetoothActivity.start(this)
        }
        bluetoothStateBroadcastReceiver.register(this)
    }

    private fun checkLocationPermission() {
        if (!locationHelper.locationPermissionsGranted()) {
            ReAllowGrantLocationPermissionActivity.start(this)
        }
    }

    private fun listenLocationChange() {
        locationProviderChangedReceiver.register(this)

        locationSubscription =
            locationProviderChangedReceiver.getLocationStatus().subscribe { isLocationEnabled ->
                if (!isLocationEnabled) {
                    ReEnableLocationActivity.start(this)
                }
            }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bluetoothStateBroadcastReceiver)
        unregisterReceiver(locationProviderChangedReceiver)
        locationSubscription?.dispose()
    }

    private val bluetoothStateBroadcastReceiver =
        BluetoothStateBroadcastReceiver { state ->
            if (state == STATE_OFF) {
                ReEnableBluetoothActivity.start(this)
            }
        }
}
