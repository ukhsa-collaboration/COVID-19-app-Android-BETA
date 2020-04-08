/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import com.example.colocate.BaseActivity
import com.example.colocate.R

class DiagnoseTemperatureActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_temperature_diagnosis)

        val radioGroup = findViewById<RadioGroup>(R.id.temperature_diagnosis_answer)

        findViewById<Button>(R.id.confirm_diagnosis).setOnClickListener {
            when (radioGroup.checkedRadioButtonId) {
                R.id.yes -> {
                    DiagnoseCoughActivity.start(this, true)
                }
                R.id.no -> {
                    DiagnoseCoughActivity.start(this, false)
                }
            }
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
