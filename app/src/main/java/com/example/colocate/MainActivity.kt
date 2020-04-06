/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.colocate.ble.BluetoothService
import com.example.colocate.ble.util.isBluetoothEnabled
import com.example.colocate.persistence.ResidentIdProvider
import com.example.colocate.status.StatusStorage
import kotlinx.android.synthetic.main.activity_main.confirm_onboarding
import kotlinx.android.synthetic.main.activity_main.explanation_link
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    protected lateinit var statusStorage: StatusStorage

    @Inject
    protected lateinit var residentIdProvider: ResidentIdProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_main)

        confirm_onboarding.setOnClickListener {
            PermissionActivity.start(this)
        }

        explanation_link.setOnClickListener {
            ExplanationActivity.start(this)
        }

        if (hasLocationPermission(this) && residentIdProvider.hasProperResidentId()) {
            if (isBluetoothEnabled()) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, BluetoothService::class.java)
                )
            }
            navigateTo(statusStorage.get())
        }
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(getIntent(context))
        }

        private fun getIntent(context: Context) =
            Intent(context, MainActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
