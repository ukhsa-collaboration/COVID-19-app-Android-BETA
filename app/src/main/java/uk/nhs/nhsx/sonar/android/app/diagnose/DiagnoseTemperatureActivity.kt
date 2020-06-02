/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.confirm_diagnosis
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.no
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.progress
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.radio_selection_error
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.temperature_description
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.temperature_diagnosis_answer
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.temperature_question
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.yes
import kotlinx.android.synthetic.main.white_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.status.DisplayState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import javax.inject.Inject

open class DiagnoseTemperatureActivity : BaseActivity() {

    @Inject
    lateinit var userStateStorage: UserStateStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature_diagnosis)

        setQuestionnaireContent()

        confirm_diagnosis.setOnClickListener {
            when (temperature_diagnosis_answer.checkedRadioButtonId) {
                R.id.yes -> {
                    nextStep(setOf(Symptom.TEMPERATURE))
                }
                R.id.no -> {
                    nextStep(emptySet())
                }
                else -> {
                    radio_selection_error.visibility = View.VISIBLE
                    radio_selection_error.announceForAccessibility(getString(R.string.radio_button_temperature_error))
                }
            }
        }

        temperature_diagnosis_answer.setOnCheckedChangeListener { _, _ ->
            radio_selection_error.visibility = View.GONE
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_blue)
        supportActionBar?.setTitle(R.string.add_my_symptoms)
        toolbar.setNavigationOnClickListener { onBackPressed() }
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

    private fun nextStep(symptoms: Set<Symptom>) {
        DiagnoseCoughActivity.start(this, symptoms)
    }

    private fun setQuestionnaireContent() {
        if (isCheckinQuestionnaire()) {
            progress.text = getString(R.string.progress_one_fifth)
            progress.contentDescription = getString(R.string.page_1_of_5)
            temperature_question.text = getString(R.string.temperature_question_simplified)
            temperature_description.text = getString(R.string.temperature_description_simplified)
        } else {
            progress.text = getString(R.string.progress_one_sixth)
            progress.contentDescription = getString(R.string.page_1_of_6)
            temperature_question.text = getString(R.string.temperature_question)
            temperature_description.text = getString(R.string.temperature_description)
        }
    }

    private fun isCheckinQuestionnaire() =
        userStateStorage.get().displayState() == DisplayState.ISOLATE

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, DiagnoseTemperatureActivity::class.java)
    }
}
