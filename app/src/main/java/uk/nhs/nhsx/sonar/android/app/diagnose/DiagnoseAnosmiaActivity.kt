/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_anosmia_diagnosis.anosmia_diagnosis_answer
import kotlinx.android.synthetic.main.activity_anosmia_diagnosis.confirm_diagnosis
import kotlinx.android.synthetic.main.activity_anosmia_diagnosis.no
import kotlinx.android.synthetic.main.activity_anosmia_diagnosis.radio_selection_error
import kotlinx.android.synthetic.main.activity_anosmia_diagnosis.yes
import kotlinx.android.synthetic.main.symptom_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import javax.inject.Inject

open class DiagnoseAnosmiaActivity : BaseActivity() {

    @Inject
    lateinit var userStateStorage: UserStateStorage

    private val symptoms: Set<Symptom> by lazy { intent.getSymptoms() }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anosmia_diagnosis)

        confirm_diagnosis.setOnClickListener {
            when (anosmia_diagnosis_answer.checkedRadioButtonId) {
                R.id.yes -> {
                    nextStep(symptoms.plus(Symptom.ANOSMIA))
                }
                R.id.no -> {
                    nextStep(symptoms)
                }
                else -> {
                    radio_selection_error.visibility = View.VISIBLE
                    radio_selection_error.announceForAccessibility(getString(R.string.radio_button_temperature_error))
                }
            }
        }

        anosmia_diagnosis_answer.setOnCheckedChangeListener { _, _ ->
            radio_selection_error.visibility = View.GONE
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
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

    protected open fun nextStep(symptoms: Set<Symptom>) {
        DiagnoseSneezeActivity.start(this, symptoms)
    }

    companion object {
        fun start(context: Context, symptoms: Set<Symptom>) =
            context.startActivity(getIntent(context, symptoms))

        private fun getIntent(context: Context, symptoms: Set<Symptom>) =
            Intent(context, DiagnoseAnosmiaActivity::class.java).apply {
                putSymptoms(symptoms)
            }
    }
}
