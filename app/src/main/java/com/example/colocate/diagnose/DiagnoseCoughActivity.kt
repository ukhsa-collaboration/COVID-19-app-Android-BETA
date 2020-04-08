/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import com.example.colocate.BaseActivity
import com.example.colocate.R
import kotlinx.android.synthetic.main.activity_cough_diagnosis.radio_selection_error
import kotlinx.android.synthetic.main.symptom_banner.close_btn

class DiagnoseCoughActivity : BaseActivity() {

    private val hasTemperature: Boolean by lazy {
        intent.getBooleanExtra(HAS_TEMPERATURE, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cough_diagnosis)

        close_btn.setImageDrawable(getDrawable(R.drawable.ic_chevron_left))
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
