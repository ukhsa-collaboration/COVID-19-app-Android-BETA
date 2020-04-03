/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.colocate.ble.util.isBluetoothEnabled
import com.example.colocate.registration.RegistrationActivity

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
                RegistrationActivity.start(this)
            } else {
                requestEnablingBluetooth()
            }
        } else {
            showToast()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                RegistrationActivity.start(this)
            } else {
                showToast()
            }
        }
    }

    private fun requestEnablingBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    private fun showToast() {
        Toast
            .makeText(this, R.string.permissions_required, Toast.LENGTH_LONG)
            .show()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(getIntent(context))
        }

        private fun getIntent(context: Context) =
            Intent(context, PermissionActivity::class.java)
    }
}
