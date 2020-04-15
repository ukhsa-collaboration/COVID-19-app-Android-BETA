/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import kotlinx.android.synthetic.main.activity_cough_diagnosis.radio_selection_error
import kotlinx.android.synthetic.main.symptom_banner.close_btn
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R

class DiagnoseCoughActivity : BaseActivity() {

    private val hasTemperature: Boolean by lazy {
        intent.getBooleanExtra(HAS_TEMPERATURE, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cough_diagnosis)

        close_btn.setImageDrawable(getDrawable(R.drawable.ic_arrow_back))
        close_btn.setOnClickListener {
            onBackPressed()
        }

        val radioGroup = findViewById<RadioGroup>(R.id.cough_diagnosis_answer)

        findViewById<Button>(R.id.confirm_diagnosis).setOnClickListener {
            when (radioGroup.checkedRadioButtonId) {
                R.id.yes -> {
                    DiagnoseReviewActivity.start(this, hasTemperature, true)
                }
                R.id.no -> {
                    DiagnoseReviewActivity.start(this, hasTemperature, false)
                }
                else -> {
                    radio_selection_error.visibility = View.VISIBLE
                    radio_selection_error.announceForAccessibility(getString(R.string.radio_button_cough_error))
                }
            }
        }

        radioGroup.setOnCheckedChangeListener { _, _ ->
            radio_selection_error.visibility = View.GONE
        }
    }

    companion object {

        const val HAS_TEMPERATURE = "HAS_TEMPERATURE"

        fun start(context: Context, hasTemperature: Boolean = false) =
            context.startActivity(
                getIntent(
                    context,
                    hasTemperature
                )
            )

        private fun getIntent(context: Context, hasTemperature: Boolean) =
            Intent(context, DiagnoseCoughActivity::class.java).apply {
                putExtra(HAS_TEMPERATURE, hasTemperature)
            }
    }
}
