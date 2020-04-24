/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose.review

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.view.View
import android.widget.AdapterView
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
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
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.diagnose.review.spinner.SpinnerAdapter
import uk.nhs.nhsx.sonar.android.app.status.StateFactory
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import uk.nhs.nhsx.sonar.android.app.util.toUiSpinnerFormat
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

    private var symptomsDate: LocalDate? = null

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

                updateStateAndNavigate()
                submission_error.visibility = View.GONE
            } else {
                submission_error.visibility = View.VISIBLE
                submission_error.announceForAccessibility(getString(R.string.submission_error))
                submit_diagnosis.text = getString(R.string.retry)
            }
        })

        submit_diagnosis.setOnClickListener {
            val selectedSymptomsDate = this.symptomsDate

            if (selectedSymptomsDate == null) {
                date_selection_error.visibility = View.VISIBLE
                date_selection_error.announceForAccessibility(getString(R.string.date_selection_error))
            } else {
                viewModel.uploadContactEvents(selectedSymptomsDate.toDateTime(LocalTime.now(), UTC))
            }
        }
    }

    private fun setDateSpinner() {
        val now = DateTime.now()
        val adapter = SpinnerAdapter(this)

        val dateValidator = object : CalendarConstraints.DateValidator {
            override fun isValid(timestamp: Long): Boolean {
                val selectedDate = localDateFromMidnightUtcTimestamp(timestamp)
                val tomorrow = LocalDate.now().plusDays(1)
                val minimum = tomorrow.minusDays(28)
                return selectedDate.isAfter(minimum) && selectedDate.isBefore(tomorrow)
            }

            override fun writeToParcel(dest: Parcel?, flags: Int) = Unit
            override fun describeContents(): Int = 0
        }

        val calendarConstraints =
            CalendarConstraints.Builder()
                .setStart(now.minusDays(28).millis)
                .setEnd(now.millis)
                .setValidator(dateValidator)
                .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(now.millis)
            .setCalendarConstraints(calendarConstraints)
            .build()

        picker.addOnPositiveButtonClickListener { timestamp ->
            val selectedDate = localDateFromMidnightUtcTimestamp(timestamp)
            adapter.update(selectedDate.toUiSpinnerFormat())
            symptoms_date_spinner.setSelection(SpinnerAdapter.MAX_VISIBLE_POSITION + 1)
            symptomsDate = selectedDate
        }

        picker.addOnCancelListener {
            symptoms_date_spinner.setSelection(adapter.count - 1)
            symptomsDate = null
        }

        picker.addOnNegativeButtonClickListener {
            symptoms_date_spinner.setSelection(adapter.count - 1)
            symptomsDate = null
        }

        symptoms_date_spinner.adapter = adapter
        symptoms_date_spinner.setSelection(adapter.count - 1)

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
                    when {
                        position == SpinnerAdapter.MAX_VISIBLE_POSITION -> {
                            picker.show(supportFragmentManager, null)
                        }
                        position < adapter.count - 1 -> {
                            date_selection_error.visibility = View.GONE
                            symptomsDate = LocalDate.now().minusDays(position)
                        }
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

    private fun updateStateAndNavigate() {
        symptomsDate?.let {
            val state = StateFactory.decide(it, symptoms)
            stateStorage.update(state)

            Timber.d("Updated the state to: $state")

            navigateTo(state)
        }
    }

    companion object {

        const val HAS_TEMPERATURE = "HAS_TEMPERATURE"

        const val HAS_COUGH = "HAS_COUGH"

        fun start(context: Context, hasTemperature: Boolean = false, hasCough: Boolean = false) =
            context.startActivity(
                getIntent(
                    context,
                    hasTemperature,
                    hasCough
                )
            )

        private fun getIntent(context: Context, hasTemperature: Boolean, hasCough: Boolean) =
            Intent(context, DiagnoseReviewActivity::class.java).apply {
                putExtra(HAS_COUGH, hasCough)
                putExtra(HAS_TEMPERATURE, hasTemperature)
            }
    }
}
