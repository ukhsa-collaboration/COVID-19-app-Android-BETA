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
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.android.synthetic.main.activity_review_diagnosis.date_selection_error
import kotlinx.android.synthetic.main.activity_review_diagnosis.review_answer_anosmia
import kotlinx.android.synthetic.main.activity_review_diagnosis.review_answer_cough
import kotlinx.android.synthetic.main.activity_review_diagnosis.review_answer_sneeze
import kotlinx.android.synthetic.main.activity_review_diagnosis.review_answer_stomach
import kotlinx.android.synthetic.main.activity_review_diagnosis.review_answer_temperature
import kotlinx.android.synthetic.main.activity_review_diagnosis.scrollView
import kotlinx.android.synthetic.main.activity_review_diagnosis.submit_diagnosis
import kotlinx.android.synthetic.main.activity_review_diagnosis.symptoms_date_spinner
import kotlinx.android.synthetic.main.white_banner.toolbar
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseSubmitActivity
import uk.nhs.nhsx.sonar.android.app.diagnose.getSymptoms
import uk.nhs.nhsx.sonar.android.app.diagnose.putSymptoms
import uk.nhs.nhsx.sonar.android.app.diagnose.review.spinner.SpinnerAdapter
import uk.nhs.nhsx.sonar.android.app.diagnose.review.spinner.setError
import uk.nhs.nhsx.sonar.android.app.diagnose.review.spinner.setFocused
import uk.nhs.nhsx.sonar.android.app.diagnose.review.spinner.setInitial
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.util.scrollToView
import uk.nhs.nhsx.sonar.android.app.util.setNavigateUpToolbar
import uk.nhs.nhsx.sonar.android.app.util.toUiSpinnerFormat

class DiagnoseReviewActivity : BaseActivity() {

    private val symptoms: Set<Symptom> by lazy { intent.getSymptoms() }

    private var symptomsDate: DateTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_review_diagnosis)

        setNavigateUpToolbar(toolbar, R.string.add_my_symptoms)

        setSymptomsReviewAnswers()
        setDateSpinner()

        submit_diagnosis.setOnClickListener {
            val selectedSymptomsDate = this.symptomsDate

            if (selectedSymptomsDate == null) {
                date_selection_error.visibility = View.VISIBLE
                symptoms_date_spinner.setError()
                date_selection_error.announceForAccessibility(getString(R.string.date_selection_error))
                scrollView.scrollToView(date_selection_error)
            } else {
                DiagnoseSubmitActivity.start(this, symptoms, selectedSymptomsDate)
            }
        }
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled) {
            submit_diagnosis.setBackgroundResource(R.drawable.button_round_background_inversed)
        } else {
            submit_diagnosis.setBackgroundResource(R.drawable.button_round_background)
        }
    }

    private fun setDateSpinner() {
        val adapter = SpinnerAdapter(this)

        val dateValidator = object : CalendarConstraints.DateValidator {
            override fun isValid(timestamp: Long): Boolean {
                val selectedDate = DateTime(timestamp, UTC).toLocalDate()
                val tomorrow = LocalDate.now().plusDays(1)
                val minimum = tomorrow.minusDays(28)

                return selectedDate.isAfter(minimum) && selectedDate.isBefore(tomorrow)
            }

            override fun writeToParcel(dest: Parcel?, flags: Int) = Unit
            override fun describeContents(): Int = 0
        }

        val calendarConstraints =
            CalendarConstraints.Builder()
                .setStart(DateTime.now().minusDays(28).millis)
                .setEnd(DateTime.now().millis)
                .setValidator(dateValidator)
                .build()

        val today = DateTime.now()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(today.millis + today.zone.getOffset(today.millis))
            .setCalendarConstraints(calendarConstraints)
            .build()

        picker.addOnPositiveButtonClickListener { timestamp ->
            val selectedDate = DateTime(timestamp, UTC)
            adapter.update(selectedDate.toLocalDate().toUiSpinnerFormat())
            symptoms_date_spinner.setSelection(SpinnerAdapter.MAX_VISIBLE_POSITION + 1)
            symptoms_date_spinner.setFocused()
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
                        position < SpinnerAdapter.MAX_VISIBLE_POSITION -> {
                            date_selection_error.visibility = View.GONE
                            symptomsDate = DateTime.now(UTC).minusDays(position)
                            symptoms_date_spinner.setFocused()
                        }
                        position == (adapter.count - 1) -> {
                            symptoms_date_spinner.setInitial()
                        }
                    }
                }
            }
    }

    private fun setSymptomsReviewAnswers() {
        review_answer_temperature.text =
            when (Symptom.TEMPERATURE in symptoms) {
                true -> getString(R.string.i_do_temperature)
                false -> getString(R.string.i_do_not_temperature)
            }

        review_answer_cough.text =
            when (Symptom.COUGH in symptoms) {
                true -> getString(R.string.i_do_cough)
                false -> getString(R.string.i_do_not_cough)
            }

        review_answer_anosmia.text =
            when (Symptom.ANOSMIA in symptoms) {
                true -> getString(R.string.i_do_anosmia)
                false -> getString(R.string.i_do_not_anosmia)
            }

        review_answer_sneeze.text =
            when (Symptom.SNEEZE in symptoms) {
                true -> getString(R.string.i_do_sneeze)
                false -> getString(R.string.i_do_not_sneeze)
            }

        review_answer_stomach.text =
            when (Symptom.NAUSEA in symptoms) {
                true -> getString(R.string.i_do_stomach)
                false -> getString(R.string.i_do_not_stomach)
            }
    }

    companion object {
        fun start(context: Context, symptoms: Set<Symptom>) =
            context.startActivity(getIntent(context, symptoms))

        private fun getIntent(context: Context, symptoms: Set<Symptom>) =
            Intent(context, DiagnoseReviewActivity::class.java).apply {
                putSymptoms(symptoms)
            }
    }
}
