/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_stomach_diagnosis.confirm_diagnosis
import kotlinx.android.synthetic.main.activity_stomach_diagnosis.no
import kotlinx.android.synthetic.main.activity_stomach_diagnosis.progress
import kotlinx.android.synthetic.main.activity_stomach_diagnosis.radio_selection_error
import kotlinx.android.synthetic.main.activity_stomach_diagnosis.stomach_description
import kotlinx.android.synthetic.main.activity_stomach_diagnosis.stomach_diagnosis_answer
import kotlinx.android.synthetic.main.activity_stomach_diagnosis.stomach_question
import kotlinx.android.synthetic.main.activity_stomach_diagnosis.yes
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.scrollView
import kotlinx.android.synthetic.main.white_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.diagnose.review.DiagnoseReviewActivity
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.DisplayState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import uk.nhs.nhsx.sonar.android.app.util.scrollToView
import uk.nhs.nhsx.sonar.android.app.util.setNavigateUpToolbar
import uk.nhs.nhsx.sonar.android.app.widgets.setRawText
import javax.inject.Inject

open class DiagnoseStomachActivity : BaseActivity() {

    @Inject
    lateinit var userStateStorage: UserStateStorage

    @Inject
    lateinit var userInbox: UserInbox

    private val symptoms: Set<Symptom> by lazy { intent.getSymptoms() }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stomach_diagnosis)

        setQuestionnaireContent()

        confirm_diagnosis.setOnClickListener {
            when (stomach_diagnosis_answer.checkedRadioButtonId) {
                R.id.yes -> nextStep(symptoms.plus(Symptom.NAUSEA))
                R.id.no -> nextStep(symptoms)
                else -> {
                    radio_selection_error.visibility = View.VISIBLE
                    radio_selection_error.announceForAccessibility(getString(R.string.radio_button_cough_error))
                    scrollView.scrollToView(radio_selection_error)
                }
            }
        }

        stomach_diagnosis_answer.setOnCheckedChangeListener { _, _ ->
            radio_selection_error.visibility = View.GONE
        }

        setNavigateUpToolbar(toolbar, R.string.add_my_symptoms)
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
        if (isCheckinQuestionnaire()) {
            progress.text = getString(R.string.progress_five_fifth)
            progress.contentDescription = getString(R.string.page_5_of_5)
            confirm_diagnosis.text = getString(R.string.submit)
            stomach_question.text = getString(R.string.stomach_question_simplified)
            stomach_description.setRawText(getString(R.string.stomach_description_simplified))

            setTitle(R.string.stomach_question_simplified)
        } else {
            progress.text = getString(R.string.progress_five_sixth)
            confirm_diagnosis.text = getString(R.string.continue_button)
            progress.contentDescription = getString(R.string.page_5_of_6)
            stomach_question.text = getString(R.string.stomach_question)
            stomach_description.setRawText(getString(R.string.stomach_description))

            setTitle(R.string.stomach_question)
        }
    }

    private fun nextStep(symptoms: Set<Symptom>) {
        if (isCheckinQuestionnaire()) {
            diagnoseForCheckin(symptoms)
        } else {
            when (UserStateTransitions.isSymptomatic(symptoms)) {
                true -> DiagnoseReviewActivity.start(this, symptoms)
                else -> DiagnoseCloseActivity.start(this)
            }
        }
    }

    private fun diagnoseForCheckin(symptoms: Set<Symptom>) {
        val newState = UserStateTransitions.diagnoseForCheckin(userStateStorage.get(), symptoms)
        if (newState is DefaultState && symptoms.isNotEmpty()) {
            userInbox.addRecovery()
        }
        userStateStorage.set(newState)
        navigateTo(newState)
    }

    private fun isCheckinQuestionnaire() =
        userStateStorage.get().displayState() == DisplayState.ISOLATE

    companion object {
        fun start(context: Context, symptoms: Set<Symptom>) =
            context.startActivity(getIntent(context, symptoms))

        private fun getIntent(context: Context, symptoms: Set<Symptom>) =
            Intent(context, DiagnoseStomachActivity::class.java).apply {
                putSymptoms(symptoms)
            }
    }
}
