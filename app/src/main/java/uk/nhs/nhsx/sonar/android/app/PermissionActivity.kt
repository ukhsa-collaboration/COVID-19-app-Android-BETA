/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothResult.Enabled
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothResult.NotApplicable
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothResult.Rejected
import uk.nhs.nhsx.sonar.android.app.ble.checkBluetoothResult
import uk.nhs.nhsx.sonar.android.app.ble.isBluetoothEnabled
import uk.nhs.nhsx.sonar.android.app.ble.requestEnablingBluetooth
import uk.nhs.nhsx.sonar.android.app.status.OkActivity

class PermissionActivity : AppCompatActivity(R.layout.activity_permission) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<AppCompatButton>(R.id.permission_continue).setOnClickListener {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (
            requestCode == REQUEST_LOCATION &&
            grantResults.size == 2 &&
            grantResults.first() == PackageManager.PERMISSION_GRANTED &&
            grantResults.last() == PackageManager.PERMISSION_GRANTED
        ) {
            if (isBluetoothEnabled()) {
                startOkActivity()
            } else {
                requestEnablingBluetooth()
            }
        } else {
            showToast()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (checkBluetoothResult(requestCode, resultCode)) {
            Enabled -> startOkActivity()
            Rejected -> showToast()
            NotApplicable -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showToast() =
        showLongToast(R.string.permissions_required)

    private fun startOkActivity() {
        OkActivity.start(this)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(
                getIntent(
                    context
                )
            )

        private fun getIntent(context: Context) =
            Intent(context, PermissionActivity::class.java)
    }
}
