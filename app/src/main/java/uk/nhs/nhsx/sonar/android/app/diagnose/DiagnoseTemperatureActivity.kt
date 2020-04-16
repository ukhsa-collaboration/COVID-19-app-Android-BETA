/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.confirm_diagnosis
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.radio_selection_error
import kotlinx.android.synthetic.main.activity_temperature_diagnosis.temperature_diagnosis_answer
import kotlinx.android.synthetic.main.symptom_banner.close_btn
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R

class DiagnoseTemperatureActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_temperature_diagnosis)

        confirm_diagnosis.setOnClickListener {
            when (temperature_diagnosis_answer.checkedRadioButtonId) {
                R.id.yes -> {
                    DiagnoseCoughActivity.start(this, true)
                }
                R.id.no -> {
                    DiagnoseCoughActivity.start(this, false)
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

        close_btn.setOnClickListener {
            onBackPressed()
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(
                getIntent(
                    context
                )
            )

        private fun getIntent(context: Context) =
            Intent(context, DiagnoseTemperatureActivity::class.java)
    }
}
