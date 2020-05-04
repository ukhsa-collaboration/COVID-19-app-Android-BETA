/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_cough_diagnosis.confirm_diagnosis
import kotlinx.android.synthetic.main.activity_cough_diagnosis.cough_diagnosis_answer
import kotlinx.android.synthetic.main.activity_cough_diagnosis.cough_question
import kotlinx.android.synthetic.main.activity_cough_diagnosis.new_cough_description
import kotlinx.android.synthetic.main.activity_cough_diagnosis.no
import kotlinx.android.synthetic.main.activity_cough_diagnosis.progress
import kotlinx.android.synthetic.main.activity_cough_diagnosis.radio_selection_error
import kotlinx.android.synthetic.main.activity_cough_diagnosis.yes
import kotlinx.android.synthetic.main.symptom_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.diagnose.review.DiagnoseReviewActivity
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.ISOLATE
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import javax.inject.Inject

class DiagnoseCoughActivity : BaseActivity() {

    private val hasTemperature: Boolean by lazy {
        intent.getBooleanExtra(HAS_TEMPERATURE, false)
    }

    @Inject
    lateinit var userStateStorage: UserStateStorage

    @Inject
    lateinit var factory: ViewModelFactory<DiagnoseCoughViewModel>

    private val viewModel by viewModels<DiagnoseCoughViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cough_diagnosis)

        setQuestionnaireContent()

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        viewModel.observeUserState().observe({ this.lifecycle }) { result ->
            when (result) {
                is StateResult.Review -> DiagnoseReviewActivity.start(this, result.symptoms)
                StateResult.Close -> DiagnoseCloseActivity.start(this)
                is StateResult.Main -> navigateTo(result.userState)
            }
        }

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

    override fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled) {
            yes.setBackgroundResource(R.drawable.radio_button_background_selector_inverse)
            no.setBackgroundResource(R.drawable.radio_button_background_selector_inverse)
            confirm_diagnosis.setBackgroundResource(R.drawable.button_round_background_inversed)
        } else {
            yes.setBackgroundResource(R.drawable.radio_button_background_selector)
            no.setBackgroundResource(R.drawable.radio_button_background_selector)
            confirm_diagnosis.setBackgroundResource(R.drawable.button_round_background)
        }
    }

    private fun setQuestionnaireContent() {
        val state = userStateStorage.get()

        if (state.displayState() == ISOLATE) {
            progress.text = getString(R.string.progress_two_out_of_two)
            progress.contentDescription = getString(R.string.page2of2)
            confirm_diagnosis.text = getString(R.string.submit)
            new_cough_description.visibility = View.GONE
            cough_question.text = getString(R.string.cough_question_simplified)
        } else {
            progress.text = getString(R.string.progress_two_thirds)
            confirm_diagnosis.text = getString(R.string.continue_button)
            progress.contentDescription = getString(R.string.page2of3)
            new_cough_description.visibility = View.VISIBLE
            cough_question.text = getString(R.string.cough_question)
        }
    }

    companion object {

        private const val HAS_TEMPERATURE = "HAS_TEMPERATURE"

        fun start(context: Context, hasTemperature: Boolean = false) =
            context.startActivity(getIntent(context, hasTemperature))

        private fun getIntent(context: Context, hasTemperature: Boolean) =
            Intent(context, DiagnoseCoughActivity::class.java).apply {
                putExtra(HAS_TEMPERATURE, hasTemperature)
            }
    }
}
