/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_cough_diagnosis.confirm_diagnosis
import kotlinx.android.synthetic.main.activity_cough_diagnosis.cough_diagnosis_answer
import kotlinx.android.synthetic.main.activity_cough_diagnosis.progress
import kotlinx.android.synthetic.main.activity_cough_diagnosis.radio_selection_error
import kotlinx.android.synthetic.main.symptom_banner.close_btn
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.diagnose.review.DiagnoseReviewActivity
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import javax.inject.Inject

class DiagnoseCoughActivity : BaseActivity() {

    private val hasTemperature: Boolean by lazy {
        intent.getBooleanExtra(HAS_TEMPERATURE, false)
    }

    @Inject
    lateinit var stateStorage: StateStorage

    @Inject
    lateinit var factory: ViewModelFactory<DiagnoseCoughViewModel>

    private val viewModel by viewModels<DiagnoseCoughViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cough_diagnosis)

        if (stateStorage.get() is RedState) {
            progress.text = getString(R.string.progress_two_out_of_two)
            confirm_diagnosis.text = getString(R.string.submit)
        } else {
            progress.text = getString(R.string.progress_two_thirds)
            confirm_diagnosis.text = getString(R.string.continue_button)
        }

        close_btn.setImageDrawable(getDrawable(R.drawable.ic_arrow_back))
        close_btn.setOnClickListener {
            onBackPressed()
        }

        viewModel.observeUserState().observe(this, Observer { statusResult ->
            when (statusResult) {
                is StateResult.Review -> DiagnoseReviewActivity.start(
                    this,
                    hasTemperature = hasTemperature,
                    hasCough = statusResult.hasCough
                )
                StateResult.Close -> DiagnoseCloseActivity.start(this)
                is StateResult.Main -> navigateTo(statusResult.userState)
            }
        })

        confirm_diagnosis.setOnClickListener {
            when (cough_diagnosis_answer.checkedRadioButtonId) {
                R.id.yes -> {
                    viewModel.update(hasTemperature, true)
                }
                R.id.no -> {
                    viewModel.update(hasTemperature, false)
                }
                else -> {
                    radio_selection_error.visibility = View.VISIBLE
                    radio_selection_error.announceForAccessibility(getString(R.string.radio_button_cough_error))
                }
            }
        }

        cough_diagnosis_answer.setOnCheckedChangeListener { _, _ ->
            radio_selection_error.visibility = View.GONE
        }
    }

    companion object {

        const val HAS_TEMPERATURE = "HAS_TEMPERATURE"

        fun start(context: Context, hasTemperature: Boolean = false) =
            context.startActivity(getIntent(context, hasTemperature))

        private fun getIntent(context: Context, hasTemperature: Boolean) =
            Intent(context, DiagnoseCoughActivity::class.java).apply {
                putExtra(HAS_TEMPERATURE, hasTemperature)
            }
    }
}
