/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_ok.registrationPanel
import kotlinx.android.synthetic.main.activity_ok.status_not_feeling_well
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.startBluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.persistence.SonarIdProvider
import javax.inject.Inject

class OkActivity : BaseActivity() {

    @Inject
    lateinit var statusStorage: StatusStorage

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<OkViewModel>

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    private val viewModel: OkViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        if (sonarIdProvider.hasProperSonarId()) {
            startBluetoothService()
        }

        setContentView(R.layout.activity_ok)

        findViewById<TextView>(R.id.status_not_feeling_well).setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }

        registrationPanel.setRetryListener {
            viewModel.register()
        }

        addViewModelListener()
        viewModel.onStart()
    }

    private fun addViewModelListener() {
        viewModel.viewState().observe(this, Observer { result ->
            when (result) {
                ViewState.Success -> {
                    registrationPanel.setState(RegistrationProgressPanel.State.REGISTERED)
                    startBluetoothService()
                    status_not_feeling_well.isClickable = true
                    status_not_feeling_well.isFocusable = true
                    status_not_feeling_well.isEnabled = true
                }
                ViewState.Progress -> {
                    registrationPanel.setState(RegistrationProgressPanel.State.IN_PROGRESS)
                }
                is ViewState.Error -> {
                    registrationPanel.setState(RegistrationProgressPanel.State.FAILED)
                }
            }
        })
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
