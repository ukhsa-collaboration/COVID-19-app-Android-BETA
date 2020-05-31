/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.edgecases

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_device_not_supported.bleInfoUlr
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.URL_NHS_NOT_SUPPORTED_DEVICE
import uk.nhs.nhsx.sonar.android.app.util.openUrl

class DeviceNotSupportedActivity : AppCompatActivity(R.layout.activity_device_not_supported) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleInfoUlr.setOnClickListener {
            openUrl(URL_NHS_NOT_SUPPORTED_DEVICE)
        }
    }

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        fun getIntent(context: Context) = Intent(context, DeviceNotSupportedActivity::class.java)
    }
}
