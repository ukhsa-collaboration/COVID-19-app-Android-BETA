/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_permission.permission_continue
import uk.nhs.nhsx.sonar.android.app.ColorInversionAwareActivity
import uk.nhs.nhsx.sonar.android.app.DeviceDetection
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.sonar.android.app.status.startStatusActivity
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper
import uk.nhs.nhsx.sonar.android.app.util.isBluetoothEnabled
import javax.inject.Inject

class PermissionActivity : ColorInversionAwareActivity(R.layout.activity_permission) {

    @Inject
    lateinit var locationHelper: LocationHelper

    @Inject
    lateinit var deviceDetection: DeviceDetection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        permission_continue.setOnClickListener {
            checkRequirements()
        }
    }

    private fun checkRequirements() {
        if (!isBluetoothEnabled()) {
            requestEnablingBluetooth()
            return
        }
        if (deviceDetection.isUnsupported()) {
            DeviceNotSupportedActivity.start(this)
            finish()
            return
        }
        if (!locationHelper.locationPermissionsGranted()) {
            requestLocationPermissions()
            return
        }
        if (!locationHelper.isLocationEnabled()) {
            EnableLocationActivity.start(this)
            return
        }
        startStatusActivity()
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled) {
            permission_continue.setBackgroundResource(R.drawable.button_round_background_inversed)
        } else {
            permission_continue.setBackgroundResource(R.drawable.button_round_background)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (isBluetoothEnabled()) {
            checkRequirements()
        } else {
            EnableBluetoothActivity.start(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (locationHelper.locationPermissionsGranted()) {
            checkRequirements()
        } else {
            GrantLocationPermissionActivity.start(this)
        }
    }

    private fun requestEnablingBluetooth() {
        if (!isBluetoothEnabled()) {
            startActivityForResult(Intent(ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT)
        }
    }

    private fun requestLocationPermissions() {
        requestPermissions(locationHelper.requiredLocationPermissions, REQUEST_LOCATION)
    }

    companion object {
        const val REQUEST_ENABLE_BT: Int = 47
        const val REQUEST_LOCATION: Int = 75

        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, PermissionActivity::class.java)
    }
}
