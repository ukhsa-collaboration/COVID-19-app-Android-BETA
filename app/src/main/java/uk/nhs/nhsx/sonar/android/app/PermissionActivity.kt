/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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
