/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_at_risk.follow_until
import kotlinx.android.synthetic.main.activity_isolate.latest_advice_red
import kotlinx.android.synthetic.main.activity_isolate.symptoms
import kotlinx.android.synthetic.main.activity_isolate.will_be_notified
import kotlinx.android.synthetic.main.status_footer_view_common.medicalWorkersInstructions
import kotlinx.android.synthetic.main.status_footer_view_common.nhs_service
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.medicalworkers.MedicalWorkersInstructionsDialog
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeDialog
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeViewModel
import uk.nhs.nhsx.sonar.android.app.util.LATEST_ADVICE_URL_RED_STATE
import uk.nhs.nhsx.sonar.android.app.util.NHS_SUPPORT_PAGE
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
import javax.inject.Inject

class IsolateActivity : BaseActivity() {

    @Inject
    protected lateinit var stateStorage: StateStorage

    private lateinit var updateSymptomsDialog: BottomSheetDialog

    @Inject
    lateinit var referenceCodeViewModelFactory: ViewModelFactory<ReferenceCodeViewModel>
    private val referenceCodeViewModel: ReferenceCodeViewModel by viewModels { referenceCodeViewModelFactory }
    private var referenceCodeDialog: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_isolate)
        BluetoothService.start(this)

        val state: RedState = stateStorage.get() as RedState

        symptoms.text = state.symptoms.sortedDescending().joinToString("\n") {
            when (it) {
                Symptom.TEMPERATURE -> getString(R.string.high_temperature)
                Symptom.COUGH -> getString(R.string.continuous_cough)
            }
        }

        follow_until.text = getString(R.string.follow_until, state.until.toUiFormat())
        will_be_notified.text =
            getString(R.string.isolate_will_be_notified, state.until.toUiFormat())

        latest_advice_red.setOnClickListener {
            openUrl(LATEST_ADVICE_URL_RED_STATE)
        }

        nhs_service.setOnClickListener {
            openUrl(NHS_SUPPORT_PAGE)
        }

        medicalWorkersInstructions.setOnClickListener {
            MedicalWorkersInstructionsDialog(this).show()
        }

        setUpdateSymptomsDialog()
        referenceCodeDialog = ReferenceCodeDialog(
            this,
            referenceCodeViewModel,
            findViewById(R.id.reference_code_link)
        )
    }

    private fun setUpdateSymptomsDialog() {
        updateSymptomsDialog = BottomSheetDialog(this, R.style.PersistentBottomSheet)
        updateSymptomsDialog.setContentView(
            layoutInflater.inflate(
                R.layout.bottom_sheet_isolate,
                null
            )
        )
        updateSymptomsDialog.behavior.isHideable = false

        updateSymptomsDialog.findViewById<Button>(R.id.no_symptoms)?.setOnClickListener {
            stateStorage.update(DefaultState())
            navigateTo(stateStorage.get())
            updateSymptomsDialog.cancel()
        }

        updateSymptomsDialog.setOnCancelListener {
            finish()
        }

        updateSymptomsDialog.findViewById<Button>(R.id.have_symptoms)?.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }
    }

    override fun onResume() {
        super.onResume()

        val state = stateStorage.get()
        navigateTo(state)

        if (state.hasExpired()) {
            updateSymptomsDialog.show()
        } else {
            updateSymptomsDialog.dismiss()
        }
    }

    override fun onPause() {
        super.onPause()
        updateSymptomsDialog.dismiss()
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
