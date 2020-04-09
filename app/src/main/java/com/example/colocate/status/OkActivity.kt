/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.AppCompatButton
import com.example.colocate.BaseActivity
import com.example.colocate.R
import com.example.colocate.appComponent
import com.example.colocate.ble.startBluetoothService
import com.example.colocate.diagnose.DiagnoseTemperatureActivity
import javax.inject.Inject

class OkActivity : BaseActivity() {

    @Inject
    protected lateinit var statusStorage: StatusStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        startBluetoothService()

        setContentView(R.layout.activity_ok)

        findViewById<AppCompatButton>(R.id.re_diagnose_button).setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }
    }

    override fun onResume() {
        super.onResume()

        navigateTo(statusStorage.get())
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(
                getIntent(
                    context
                )
            )

        private fun getIntent(context: Context) =
            Intent(context, OkActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
    }
}
