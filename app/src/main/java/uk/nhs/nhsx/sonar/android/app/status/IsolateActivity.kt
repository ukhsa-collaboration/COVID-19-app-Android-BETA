/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_isolate.latest_advice_red
import kotlinx.android.synthetic.main.activity_isolate.nhs_service
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.util.LATEST_ADVICE_URL_RED_STATE
import uk.nhs.nhsx.sonar.android.app.util.NSH_SUPPORT_PAGE
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import javax.inject.Inject

class IsolateActivity : BaseActivity() {

    @Inject
    protected lateinit var statusStorage: StatusStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_isolate)
        appComponent.inject(this)
        BluetoothService.start(this)

        latest_advice_red.setOnClickListener {
            openUrl(LATEST_ADVICE_URL_RED_STATE)
        }

        nhs_service.setOnClickListener {
            openUrl(NSH_SUPPORT_PAGE)
        }
    }

    override fun onResume() {
        super.onResume()

        navigateTo(statusStorage.get())
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, IsolateActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
