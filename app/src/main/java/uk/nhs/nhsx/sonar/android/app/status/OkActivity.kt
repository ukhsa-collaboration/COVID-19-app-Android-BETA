/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_ok.latest_advice_ok
import kotlinx.android.synthetic.main.activity_ok.notificationPanel
import kotlinx.android.synthetic.main.activity_ok.registrationPanel
import kotlinx.android.synthetic.main.activity_ok.status_not_feeling_well
import kotlinx.android.synthetic.main.activity_review_close.nhs_service
import kotlinx.android.synthetic.main.banner.toolbar_info
import kotlinx.android.synthetic.main.status_footer_view.medical_workers_card
import kotlinx.android.synthetic.main.status_footer_view.reference_link_card
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.medicalworkers.MedicalWorkersInstructionsDialog
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeActivity
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.RegistrationState.Complete
import uk.nhs.nhsx.sonar.android.app.status.RegistrationState.InProgress
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.URL_LATEST_ADVICE_DEFAULT
import uk.nhs.nhsx.sonar.android.app.util.URL_SUPPORT_DEFAULT
import uk.nhs.nhsx.sonar.android.app.util.cardColourInversion
import uk.nhs.nhsx.sonar.android.app.util.observe
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.showExpanded
import javax.inject.Inject

class OkActivity : BaseActivity() {

    @Inject
    lateinit var userStateStorage: UserStateStorage

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<OkViewModel>
    private val viewModel: OkViewModel by viewModels { viewModelFactory }

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var postCodeProvider: PostCodeProvider

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
            openUrl(URL_LATEST_ADVICE_DEFAULT)
        }

        toggleNotFeelingCard(false)

        nhs_service.setOnClickListener {
            openUrl(URL_SUPPORT_DEFAULT)
        }

        medical_workers_card.setOnClickListener {
            MedicalWorkersInstructionsDialog(this).showExpanded()
        }

        toolbar_info.setOnClickListener {
            openUrl(URL_INFO)
        }

        toggleReferenceCodeCard(false)
        reference_link_card.setOnClickListener {
            ReferenceCodeActivity.start(this)
        }

        notificationPanel.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }

        addViewModelListener()
        viewModel.onStart()

        setRecoveryDialog()
    }

    private fun toggleNotFeelingCard(enabled: Boolean) {
        with(status_not_feeling_well) {
            isClickable = enabled
            isEnabled = enabled
        }
    }

    private fun toggleReferenceCodeCard(enabled: Boolean) {
        with(reference_link_card) {
            isEnabled = enabled
            isClickable = enabled
        }
    }

    private fun addViewModelListener() {
        viewModel.viewState().observe(this) { result ->
            when (result) {
                Complete -> {
                    registrationPanel.setState(result)
                    BluetoothService.start(this)
                    toggleNotFeelingCard(true)
                    toggleReferenceCodeCard(true)
                }
                InProgress -> {
                    registrationPanel.setState(result)
                    toggleNotFeelingCard(false)
                    toggleReferenceCodeCard(false)
                }
            }
        }
    }

    private fun setRecoveryDialog() {
        recoveryDialog = BottomSheetDialog(this, R.style.PersistentBottomSheet)
        recoveryDialog.setContentView(layoutInflater.inflate(R.layout.bottom_sheet_recovery, null))
        recoveryDialog.behavior.isHideable = false

        recoveryDialog.findViewById<Button>(R.id.ok)?.setOnClickListener {
            userStateStorage.set(DefaultState)
            recoveryDialog.dismiss()
        }
        recoveryDialog.setOnCancelListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        val state = userStateStorage.get()
        navigateTo(state)

        if (state is RecoveryState) {
            recoveryDialog.showExpanded()
        } else {
            recoveryDialog.dismiss()
        }

        notificationPanel.isVisible =
            !NotificationManagerCompat.from(this).areNotificationsEnabled()
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        status_not_feeling_well.cardColourInversion(inversionModeEnabled)
        medical_workers_card.cardColourInversion(inversionModeEnabled)
        notificationPanel.cardColourInversion(inversionModeEnabled)
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
