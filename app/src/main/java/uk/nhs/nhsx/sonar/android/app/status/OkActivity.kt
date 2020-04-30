/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_ok.latest_advice_ok
import kotlinx.android.synthetic.main.activity_ok.registrationPanel
import kotlinx.android.synthetic.main.activity_ok.status_not_feeling_well
import kotlinx.android.synthetic.main.activity_review_close.nhs_service
import kotlinx.android.synthetic.main.banner.toolbar_info
import kotlinx.android.synthetic.main.status_footer_view.medical_workers_card
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.medicalworkers.MedicalWorkersInstructionsDialog
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeWorkLauncher
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.RegistrationState.Complete
import uk.nhs.nhsx.sonar.android.app.status.RegistrationState.InProgress
import uk.nhs.nhsx.sonar.android.app.util.INFO_PAGE
import uk.nhs.nhsx.sonar.android.app.util.LATEST_ADVICE_URL
import uk.nhs.nhsx.sonar.android.app.util.NHS_SUPPORT_PAGE
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import javax.inject.Inject

class OkActivity : BaseActivity() {

    @Inject
    lateinit var stateStorage: StateStorage

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<OkViewModel>
    private val viewModel: OkViewModel by viewModels { viewModelFactory }

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var postCodeProvider: PostCodeProvider

    @Inject
    lateinit var referenceCodeWorkLauncher: ReferenceCodeWorkLauncher

    private lateinit var recoveryDialog: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        if (sonarIdProvider.hasProperSonarId()) {
            BluetoothService.start(this)
        }

        setContentView(R.layout.activity_ok)

        status_not_feeling_well.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }

        latest_advice_ok.setOnClickListener {
            openUrl(LATEST_ADVICE_URL)
        }

        toggleNotFeelingCard(false)

        nhs_service.setOnClickListener {
            openUrl(NHS_SUPPORT_PAGE)
        }

        medical_workers_card.setOnClickListener {
            MedicalWorkersInstructionsDialog(this).show()
        }

        toolbar_info.setOnClickListener {
            openUrl(INFO_PAGE)
        }

        addViewModelListener()
        viewModel.onStart()

        setRecoveryDialog()
    }

    private fun toggleNotFeelingCard(enabled: Boolean) {
        status_not_feeling_well.let {
            it.isClickable = enabled
            it.isEnabled = enabled
        }
    }

    private fun addViewModelListener() {
        viewModel.viewState().observe(this, Observer { result ->
            when (result) {
                Complete -> {
                    registrationPanel.setState(result)
                    BluetoothService.start(this)
                    toggleNotFeelingCard(true)
                    referenceCodeWorkLauncher.launchWork()
                }
                InProgress -> {
                    registrationPanel.setState(result)
                    toggleNotFeelingCard(false)
                }
                null -> {
                }
            }
        })
    }

    private fun setRecoveryDialog() {
        recoveryDialog = BottomSheetDialog(this, R.style.PersistentBottomSheet)
        recoveryDialog.setContentView(layoutInflater.inflate(R.layout.bottom_sheet_recovery, null))
        recoveryDialog.behavior.isHideable = false

        recoveryDialog.findViewById<Button>(R.id.ok)?.setOnClickListener {
            stateStorage.update(DefaultState())
            recoveryDialog.dismiss()
        }
        recoveryDialog.setOnCancelListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        val state = stateStorage.get()
        navigateTo(state)

        if (state is RecoveryState) {
            recoveryDialog.show()
            recoveryDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            recoveryDialog.dismiss()
        }
    }

    override fun onPause() {
        super.onPause()
        recoveryDialog.dismiss()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, OkActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
    }
}
