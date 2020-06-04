/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_cough_diagnosis.confirm_diagnosis
import kotlinx.android.synthetic.main.activity_cough_diagnosis.cough_description
import kotlinx.android.synthetic.main.activity_cough_diagnosis.cough_diagnosis_answer
import kotlinx.android.synthetic.main.activity_cough_diagnosis.cough_question
import kotlinx.android.synthetic.main.activity_cough_diagnosis.no
import kotlinx.android.synthetic.main.activity_cough_diagnosis.progress
import kotlinx.android.synthetic.main.activity_cough_diagnosis.radio_selection_error
import kotlinx.android.synthetic.main.activity_cough_diagnosis.scrollView
import kotlinx.android.synthetic.main.activity_cough_diagnosis.yes
import kotlinx.android.synthetic.main.white_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.status.DisplayState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.util.announce
import uk.nhs.nhsx.sonar.android.app.util.scrollToView
import uk.nhs.nhsx.sonar.android.app.util.setNavigateUpToolbar
import uk.nhs.nhsx.sonar.android.app.widgets.setRawText
import javax.inject.Inject

class DiagnoseCoughActivity : BaseActivity() {

    private val symptoms: Set<Symptom> by lazy { intent.getSymptoms() }

    @Inject
    lateinit var userStateStorage: UserStateStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cough_diagnosis)

        setQuestionnaireContent()

        setNavigateUpToolbar(toolbar, R.string.add_my_symptoms)

        confirm_diagnosis.setOnClickListener {
            when (cough_diagnosis_answer.checkedRadioButtonId) {
                R.id.yes -> nextStep(symptoms.plus(Symptom.COUGH))
                R.id.no -> nextStep(symptoms)
                else -> {
                    radio_selection_error.visibility = View.VISIBLE
                    radio_selection_error.announceForAccessibility(getString(R.string.radio_button_cough_error))
                    scrollView.scrollToView(radio_selection_error)
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

    private fun nextStep(symptoms: Set<Symptom>) {
        DiagnoseAnosmiaActivity.start(this, symptoms)
    }

    private fun setQuestionnaireContent() {
        if (isCheckinQuestionnaire()) {
            progress.text = getString(R.string.progress_two_fifth)
            progress.contentDescription = getString(R.string.page_2_of_5)
            cough_question.text = getString(R.string.cough_question_simplified)
            cough_description.setRawText(getString(R.string.cough_description_simplified))

            yes.text = getString(R.string.yes_cough)
            no.text = getString(R.string.no_cough)

            announce(R.string.cough_question_simplified)
        } else {
            progress.text = getString(R.string.progress_two_sixth)
            progress.contentDescription = getString(R.string.page_2_of_6)
            cough_question.text = getString(R.string.cough_question)
            cough_description.setRawText(getString(R.string.cough_description))

            yes.text = getString(R.string.yes_new_cough)
            no.text = getString(R.string.no_new_cough)

            announce(R.string.cough_question)
        }
    }

    private fun isCheckinQuestionnaire() =
        userStateStorage.get().displayState() == DisplayState.ISOLATE

    companion object {

        fun start(context: Context, symptoms: Set<Symptom>) =
            context.startActivity(getIntent(context, symptoms))

        private fun getIntent(context: Context, symptoms: Set<Symptom>) =
            Intent(context, DiagnoseCoughActivity::class.java).apply {
                putSymptoms(symptoms)
            }
    }
}
