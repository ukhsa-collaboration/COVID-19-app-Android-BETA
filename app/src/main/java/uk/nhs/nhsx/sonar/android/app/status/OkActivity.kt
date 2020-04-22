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
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_ok.latest_advice_ok
import kotlinx.android.synthetic.main.activity_ok.registrationPanel
import kotlinx.android.synthetic.main.activity_ok.status_not_feeling_well
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.util.LATEST_ADVICE_URL
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import javax.inject.Inject

class OkActivity : BaseActivity() {

    @Inject
    lateinit var stateStorage: StateStorage

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<OkViewModel>

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    private val viewModel: OkViewModel by viewModels {
        viewModelFactory
    }

    private lateinit var dialog: BottomSheetDialog

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

        registrationPanel.setRetryListener {
            viewModel.register()
        }

        latest_advice_ok.setOnClickListener {
            openUrl(LATEST_ADVICE_URL)
        }

        toggleNotFeelingCard(false)

        addViewModelListener()
        viewModel.onStart()

        setBottomSheet()
    }

    private fun toggleNotFeelingCard(enabled: Boolean) {
        status_not_feeling_well.let {
            it.isClickable = enabled
            it.isFocusable = enabled
            it.isEnabled = enabled
        }
    }

    private fun addViewModelListener() {
        viewModel.viewState().observe(this, Observer { result ->
            when (result) {
                ViewState.Success -> {
                    registrationPanel.setState(RegistrationProgressPanel.State.REGISTERED)
                    BluetoothService.start(this)
                    toggleNotFeelingCard(true)
                }
                ViewState.Progress -> {
                    registrationPanel.setState(RegistrationProgressPanel.State.IN_PROGRESS)
                    toggleNotFeelingCard(false)
                }
                is ViewState.Error -> {
                    registrationPanel.setState(RegistrationProgressPanel.State.FAILED)
                    toggleNotFeelingCard(false)
                }
            }
        })
    }

    private fun setBottomSheet() {
        dialog = BottomSheetDialog(this, R.style.PersistentBottomSheet)
        dialog.setContentView(layoutInflater.inflate(R.layout.bottom_sheet_recovery, null))
        dialog.behavior.isHideable = false

        dialog.findViewById<Button>(R.id.ok)?.setOnClickListener {
            stateStorage.update(DefaultState(DateTime.now(DateTimeZone.UTC)))
            dialog.dismiss()
        }
        dialog.setOnCancelListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        navigateTo(stateStorage.get())

        if (stateStorage.get() is RecoveryState) {
            dialog.show()
        } else {
            dialog.dismiss()
        }
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
