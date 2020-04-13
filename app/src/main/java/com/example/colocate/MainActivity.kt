/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.colocate.ble.startBluetoothService
import com.example.colocate.persistence.SonarIdProvider
import com.example.colocate.status.StatusStorage
import com.example.colocate.status.navigateTo
import kotlinx.android.synthetic.main.activity_main.confirm_onboarding
import kotlinx.android.synthetic.main.activity_main.explanation_link
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var statusStorage: StatusStorage

    @Inject
    protected lateinit var sonarIdProvider: SonarIdProvider

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

        if (sonarIdProvider.hasProperSonarId()) {
            startBluetoothService()
            navigateTo(statusStorage.get())
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        fun getIntent(context: Context) =
            Intent(context, MainActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
