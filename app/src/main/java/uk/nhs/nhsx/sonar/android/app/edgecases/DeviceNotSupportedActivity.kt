/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.edgecases

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.sonar.android.app.R

class DeviceNotSupportedActivity : AppCompatActivity(R.layout.activity_device_not_supported) {

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        fun getIntent(context: Context) = Intent(context, DeviceNotSupportedActivity::class.java)
    }
}
