/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_review_diagnosis.date_selection_error
import kotlinx.android.synthetic.main.activity_review_diagnosis.review_answer_cough
import kotlinx.android.synthetic.main.activity_review_diagnosis.review_answer_temperature
import kotlinx.android.synthetic.main.activity_review_diagnosis.submission_error
import kotlinx.android.synthetic.main.activity_review_diagnosis.submit_diagnosis
import kotlinx.android.synthetic.main.activity_review_diagnosis.symptoms_date_prompt
import kotlinx.android.synthetic.main.activity_review_diagnosis.symptoms_date_spinner
import kotlinx.android.synthetic.main.symptom_banner.close_btn
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.showToast
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import javax.inject.Inject

class DiagnoseReviewActivity : BaseActivity() {
    @Inject
    protected lateinit var stateStorage: StateStorage

    @Inject
    protected lateinit var viewModelFactory: ViewModelFactory<DiagnoseReviewViewModel>

    private val viewModel: DiagnoseReviewViewModel by viewModels {
        viewModelFactory
    }

    private val symptoms: Set<Symptom> by lazy {
        setOf(
            if (intent.getBooleanExtra(HAS_COUGH, false)) COUGH else null,
            if (intent.getBooleanExtra(HAS_TEMPERATURE, false)) TEMPERATURE else null
        ).filterNotNull().toSet()
    }

    private var symptomsDate: DateTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_review_diagnosis)

        close_btn.setImageDrawable(getDrawable(R.drawable.ic_arrow_back))
        close_btn.setOnClickListener {
            onBackPressed()
        }

        setSymptomsReviewAnswers()
        setSymptomsDateQuestion()

        setDateSpinner()

        viewModel.isolationResult.observe(this, Observer
        { result ->
            if (result is ViewState.Success) {
                viewModel.clearContactEvents()

                showToast(R.string.successfull_data_upload)

                updateStatusAndNavigate()
                submission_error.visibility = View.GONE
            } else {
                submission_error.visibility = View.VISIBLE
                submission_error.announceForAccessibility(getString(R.string.submission_error))
                submit_diagnosis.text = getString(R.string.retry)
            }
        })

        submit_diagnosis.setOnClickListener {
            if (symptomsDate == null) {
                date_selection_error.visibility = View.VISIBLE
                date_selection_error.announceForAccessibility(getString(R.string.date_selection_error))
            } else {
                viewModel.uploadContactEvents()
            }
        }
    }

    private fun setDateSpinner() {
        val adapter = SpinnerAdapter(this)

        symptoms_date_spinner.adapter = adapter
        symptoms_date_spinner.setSelection(adapter.count)

        symptoms_date_spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    symptomsDate = null
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position < adapter.count) {
                        date_selection_error.visibility = View.GONE
                        symptomsDate = DateTime.now(UTC).minusDays(position)
                    }
                }
            }
    }

    private fun setSymptomsReviewAnswers() {
        review_answer_temperature.text =
            when (TEMPERATURE in symptoms) {
                true -> getString(R.string.i_do_temperature)
                false -> getString(R.string.i_do_not_temperature)
            }

        review_answer_cough.text =
            when (COUGH in symptoms) {
                true -> getString(R.string.i_do_cough)
                false -> getString(R.string.i_do_not_cough)
            }
    }

    private fun setSymptomsDateQuestion() {
        symptoms_date_prompt.text =
            when (symptoms) {
                setOf(TEMPERATURE) -> getString(R.string.symptoms_date_prompt_temperature)
                setOf(COUGH) -> getString(R.string.symptoms_date_prompt_cough)
                else -> getString(R.string.symptoms_date_prompt_all)
            }
    }

    private fun updateStatusAndNavigate() {
        symptomsDate?.let {
            val state = RedState(it.plusDays(7), symptoms)
            stateStorage.update(state)

            Timber.d("Updated the state to: $state")

            navigateTo(state)
        }
    }

    companion object {

        const val HAS_TEMPERATURE = "HAS_TEMPERATURE"

        const val HAS_COUGH = "HAS_COUGH"

        fun start(context: Context, hasTemperature: Boolean = false, hasCough: Boolean = false) =
            context.startActivity(getIntent(context, hasTemperature, hasCough))

        private fun getIntent(context: Context, hasTemperature: Boolean, hasCough: Boolean) =
            Intent(context, DiagnoseReviewActivity::class.java).apply {
                putExtra(HAS_COUGH, hasCough)
                putExtra(HAS_TEMPERATURE, hasTemperature)
            }
    }
}
