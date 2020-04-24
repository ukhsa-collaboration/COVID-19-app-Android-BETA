/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_isolate.latest_advice_red
import kotlinx.android.synthetic.main.activity_isolate.nhs_service
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.util.LATEST_ADVICE_URL_RED_STATE
import uk.nhs.nhsx.sonar.android.app.util.NHS_SUPPORT_PAGE
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import javax.inject.Inject

class IsolateActivity : BaseActivity() {

    @Inject
    protected lateinit var stateStorage: StateStorage

    private lateinit var dialog: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_isolate)
        BluetoothService.start(this)

        latest_advice_red.setOnClickListener {
            openUrl(LATEST_ADVICE_URL_RED_STATE)
        }

        nhs_service.setOnClickListener {
            openUrl(NHS_SUPPORT_PAGE)
        }

        setBottomSheet()
    }

    private fun setBottomSheet() {
        dialog = BottomSheetDialog(this, R.style.PersistentBottomSheet)
        dialog.setContentView(layoutInflater.inflate(R.layout.bottom_sheet_isolate, null))
        dialog.behavior.isHideable = false

        dialog.findViewById<Button>(R.id.no_symptoms)?.setOnClickListener {
            stateStorage.update(DefaultState())
            navigateTo(stateStorage.get())
            dialog.cancel()
        }

        dialog.setOnCancelListener {
            finish()
        }

        dialog.findViewById<Button>(R.id.have_symptoms)?.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }
    }

    override fun onResume() {
        super.onResume()

        val state = stateStorage.get()
        navigateTo(state)

        if (state.hasExpired()) {
            dialog.show()
        } else {
            dialog.dismiss()
        }
    }

    override fun onPause() {
        super.onPause()
        dialog.dismiss()
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
