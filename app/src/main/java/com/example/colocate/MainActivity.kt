/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.colocate.ble.BluetoothService
import com.example.colocate.isolate.IsolateActivity
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.StatusStorage
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    protected lateinit var statusStorage: StatusStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ColocateApplication).applicationComponent.inject(this)

        setContentView(R.layout.activity_main)

        findViewById<AppCompatButton>(R.id.confirm_onboarding).setOnClickListener {
            requestPermissions(
                arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
        }

        if (hasLocationPermission()) {
            ContextCompat.startForegroundService(this, Intent(this, BluetoothService::class.java))

            when (statusStorage.get()) {
                CovidStatus.POTENTIAL -> startActivity(Intent(this, AtRiskActivity::class.java))
                CovidStatus.RED -> startActivity(Intent(this, IsolateActivity::class.java))
                else -> startActivity(Intent(this, OkActivity::class.java))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION &&
            grantResults.size == 2 &&
            grantResults.first() == PERMISSION_GRANTED &&
            grantResults.last() == PERMISSION_GRANTED
        ) {
            startActivity(Intent(this, DiagnoseActivity::class.java))
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
}
