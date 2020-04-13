/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.Observer
import com.example.colocate.BaseActivity
import com.example.colocate.R
import com.example.colocate.ViewModelFactory
import com.example.colocate.ViewState
import com.example.colocate.appComponent
import com.example.colocate.ble.startBluetoothService
import com.example.colocate.diagnose.DiagnoseTemperatureActivity
import com.example.colocate.persistence.SonarIdProvider
import kotlinx.android.synthetic.main.activity_ok.registrationPanel
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

        findViewById<AppCompatButton>(R.id.re_diagnose_button).setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }

        registrationPanel.setRetryListener {
            viewModel.register()
        }

        addViewModelListener()
        viewModel.register()
    }

    private fun addViewModelListener() {
        viewModel.viewState().observe(this, Observer { result ->
            when (result) {
                ViewState.Success -> {
                    registrationPanel.setState(RegistrationProgressPanel.State.REGISTERED)
                    startBluetoothService()
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
