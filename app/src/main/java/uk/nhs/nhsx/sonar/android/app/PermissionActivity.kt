/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_permission.permission_continue
import uk.nhs.nhsx.sonar.android.app.status.OkActivity

class PermissionActivity : AppCompatActivity(R.layout.activity_permission) {

    companion object {
        const val REQUEST_ENABLE_BT: Int = 47
        const val REQUEST_LOCATION: Int = 75

        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, PermissionActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permission_continue.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions(
                    arrayOf(ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION),
                    REQUEST_LOCATION
                )
            } else {
                requestPermissions(
                    arrayOf(ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (
            requestCode == REQUEST_LOCATION &&
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && grantResults.size == 2 ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && grantResults.size == 1) &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            if (isBluetoothEnabled()) {
                startOkActivity()
            } else {
                requestEnablingBluetooth()
            }
        } else {
            showToast(R.string.permissions_required)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (checkBluetoothResult(requestCode, resultCode)) {
            BluetoothResult.Enabled -> startOkActivity()
            BluetoothResult.Rejected -> showToast(R.string.permissions_required)
            BluetoothResult.NotApplicable -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun isBluetoothEnabled() =
        BluetoothAdapter.getDefaultAdapter().isEnabled

    private fun requestEnablingBluetooth() {
        if (!isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private enum class BluetoothResult {
        Enabled,
        Rejected,
        NotApplicable
    }

    private fun checkBluetoothResult(requestCode: Int, resultCode: Int): BluetoothResult {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK)
            return BluetoothResult.Enabled

        if (requestCode == REQUEST_ENABLE_BT)
            return BluetoothResult.Rejected

        return BluetoothResult.NotApplicable
    }

    private fun startOkActivity() {
        OkActivity.start(this)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
