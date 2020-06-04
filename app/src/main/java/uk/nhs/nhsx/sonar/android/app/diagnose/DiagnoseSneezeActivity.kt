/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_sneeze_diagnosis.confirm_diagnosis
import kotlinx.android.synthetic.main.activity_sneeze_diagnosis.no
import kotlinx.android.synthetic.main.activity_sneeze_diagnosis.progress
import kotlinx.android.synthetic.main.activity_sneeze_diagnosis.radio_selection_error
import kotlinx.android.synthetic.main.activity_sneeze_diagnosis.sneeze_description
import kotlinx.android.synthetic.main.activity_sneeze_diagnosis.sneeze_diagnosis_answer
import kotlinx.android.synthetic.main.activity_sneeze_diagnosis.sneeze_question
import kotlinx.android.synthetic.main.activity_sneeze_diagnosis.yes
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.scrollView
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

open class DiagnoseSneezeActivity : BaseActivity() {

    @Inject
    lateinit var userStateStorage: UserStateStorage

    private val symptoms: Set<Symptom> by lazy { intent.getSymptoms() }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sneeze_diagnosis)

        setQuestionnaireContent()

        confirm_diagnosis.setOnClickListener {
            when (sneeze_diagnosis_answer.checkedRadioButtonId) {
                R.id.yes -> {
                    nextStep(symptoms.plus(Symptom.SNEEZE))
                }
                R.id.no -> {
                    nextStep(symptoms)
                }
                else -> {
                    radio_selection_error.visibility = View.VISIBLE
                    radio_selection_error.announceForAccessibility(getString(R.string.radio_button_temperature_error))
                    scrollView.scrollToView(radio_selection_error)
                }
            }
        }

        sneeze_diagnosis_answer.setOnCheckedChangeListener { _, _ ->
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

    protected open fun nextStep(symptoms: Set<Symptom>) {
        DiagnoseStomachActivity.start(this, symptoms)
    }

    private fun setQuestionnaireContent() {
        if (isCheckinQuestionnaire()) {
            progress.text = getString(R.string.progress_four_fifth)
            progress.contentDescription = getString(R.string.page_4_of_5)
            sneeze_question.text = getString(R.string.sneeze_question_simplified)
            sneeze_description.setRawText(getString(R.string.sneeze_description_simplified))

            announce(R.string.sneeze_question_simplified)
        } else {
            progress.text = getString(R.string.progress_four_sixth)
            progress.contentDescription = getString(R.string.page_4_of_6)
            sneeze_question.text = getString(R.string.sneeze_question)
            sneeze_description.setRawText(getString(R.string.sneeze_description))

            announce(R.string.sneeze_question)
        }
    }

    private fun isCheckinQuestionnaire() =
        userStateStorage.get().displayState() == DisplayState.ISOLATE

    companion object {
        fun start(context: Context, symptoms: Set<Symptom>) =
            context.startActivity(getIntent(context, symptoms))

        private fun getIntent(context: Context, symptoms: Set<Symptom>) =
            Intent(context, DiagnoseSneezeActivity::class.java).apply {
                putSymptoms(symptoms)
            }
    }
}
