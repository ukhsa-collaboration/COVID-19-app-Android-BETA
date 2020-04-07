/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import com.example.colocate.isolate.IsolateActivity
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.StatusStorage
import javax.inject.Inject

class DiagnoseActivity : BaseActivity() {
    @Inject
    protected lateinit var statusStorage: StatusStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_diagnosis)

        val radioGroup = findViewById<RadioGroup>(R.id.diagnosis_answer)

        findViewById<Button>(R.id.confirm_diagnosis).setOnClickListener {
            when (radioGroup.checkedRadioButtonId) {
                R.id.yes -> {
                    statusStorage.update(CovidStatus.RED)
                    IsolateActivity.start(this)
                }
                R.id.no -> {
                    statusStorage.update(CovidStatus.OK)
                    OkActivity.start(this)
                }
            }
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, DiagnoseActivity::class.java)
    }
}
