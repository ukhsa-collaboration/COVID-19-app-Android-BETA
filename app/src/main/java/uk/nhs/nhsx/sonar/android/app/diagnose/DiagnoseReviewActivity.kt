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
import kotlinx.android.synthetic.main.activity_review_diagnosis.confirm_diagnosis
import kotlinx.android.synthetic.main.activity_review_diagnosis.submission_error
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

    private val hasTemperature: Boolean by lazy {
        intent.getBooleanExtra(HAS_TEMPERATURE, false)
    }

    private val hasCough: Boolean by lazy {
        intent.getBooleanExtra(HAS_COUGH, false)
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

        setSymptomsQuestion()

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
                confirm_diagnosis.text = getString(R.string.retry)
            }
        })

        confirm_diagnosis.setOnClickListener {
            if (hasCough || hasTemperature) {
                viewModel.uploadContactEvents()
            } else {
                updateStatusAndNavigate()
            }
        }
    }

    private fun setDateSpinner() {
        val adapter = SpinnerAdapter(this).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        symptoms_date_spinner.adapter = adapter
        symptoms_date_spinner.setSelection(0)

        symptoms_date_spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    symptomsDate = DateTime.now(UTC)
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    symptomsDate = DateTime.now(UTC).minusDays(position)
                }
            }
    }

    private fun setSymptomsQuestion() {
        symptoms_date_prompt.text = when {
            hasCough and hasTemperature -> getString(R.string.symptoms_date_prompt_all)
            hasTemperature -> getString(R.string.symptoms_date_prompt_temperature)
            else -> getString(R.string.symptoms_date_prompt_cough)
        }
    }

    private fun updateStatusAndNavigate() {
        val symptoms = setOf(
            if (hasCough) Symptom.COUGH else null,
            if (hasTemperature) Symptom.TEMPERATURE else null
        ).filterNotNull()
            .toSet()

        stateStorage.update(
            RedState(
                symptomsDate!!.plus(7),
                symptoms
            )
        )

        Timber.d(stateStorage.get().toString())

        navigateTo(stateStorage.get())
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
