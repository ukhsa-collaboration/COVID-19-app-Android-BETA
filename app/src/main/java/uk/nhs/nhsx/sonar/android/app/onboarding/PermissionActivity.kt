/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_permission.permission_continue
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.OkActivity
import uk.nhs.nhsx.sonar.android.app.util.isLocationEnabled

class PermissionActivity : AppCompatActivity(R.layout.activity_permission) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permission_continue.setOnClickListener {
            checkRequirements()
        }
    }

    private fun checkRequirements() {
        if (!isBluetoothEnabled()) {
            requestEnablingBluetooth()
            return
        }
        if (!locationPermissionsGranted()) {
            requestLocationPermissions()
            return
        }
        if (!isLocationEnabled()) {
            startEnableLocationServicesActivity()
            return
        }
        startOkActivity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (isBluetoothEnabled()) {
            checkRequirements()
        } else {
            startEnableBluetoothActivity()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (locationPermissionsGranted()) {
            checkRequirements()
        } else {
            startGrantLocationPermissionActivity()
        }
    }

    private fun requestEnablingBluetooth() {
        if (!isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun requestLocationPermissions() {
        requestPermissions(locationPermissions, REQUEST_LOCATION)
    }

    private fun isBluetoothEnabled() =
        BluetoothAdapter.getDefaultAdapter().isEnabled

    private fun locationPermissionsGranted(): Boolean {
        return locationPermissions.all { permission ->
            packageManager.checkPermission(
                permission,
                packageName
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startOkActivity() {
        OkActivity.start(this)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun startGrantLocationPermissionActivity() {
        GrantLocationPermissionActivity.start(this)
    }

    private fun startEnableBluetoothActivity() {
        EnableBluetoothActivity.start(this)
    }

    private fun startEnableLocationServicesActivity() {
        EnableLocationServicesActivity.start(this)
    }

    companion object {
        const val REQUEST_ENABLE_BT: Int = 47
        const val REQUEST_LOCATION: Int = 75

        fun start(context: Context) =
            context.startActivity(
                getIntent(
                    context
                )
            )

        private fun getIntent(context: Context) =
            Intent(context, PermissionActivity::class.java)

        val locationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION)
        } else {
            arrayOf(ACCESS_FINE_LOCATION)
        }
    }
}
