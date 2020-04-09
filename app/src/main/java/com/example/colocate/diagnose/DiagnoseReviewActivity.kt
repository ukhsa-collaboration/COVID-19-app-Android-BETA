/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.colocate.BaseActivity
import com.example.colocate.status.OkActivity
import com.example.colocate.R
import com.example.colocate.appComponent
import com.example.colocate.status.IsolateActivity
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.StatusStorage
import kotlinx.android.synthetic.main.symptom_banner.close_btn
import javax.inject.Inject

class DiagnoseReviewActivity : BaseActivity() {
    @Inject
    protected lateinit var statusStorage: StatusStorage

    private val hasTemperature: Boolean by lazy {
        intent.getBooleanExtra(HAS_TEMPERATURE, false)
    }

    private val hasCough: Boolean by lazy {
        intent.getBooleanExtra(HAS_COUGH, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_review_diagnosis)

        close_btn.setImageDrawable(getDrawable(R.drawable.ic_chevron_left))
        close_btn.setOnClickListener {
            onBackPressed()
        }

        findViewById<Button>(R.id.confirm_diagnosis).setOnClickListener {

            if (hasCough or hasTemperature) {
                statusStorage.update(CovidStatus.RED)
                IsolateActivity.start(this)
            } else {
                statusStorage.update(CovidStatus.OK)
                OkActivity.start(this)
            }
        }
    }

    companion object {

        const val HAS_TEMPERATURE = "HAS_TEMPERATURE"

        const val HAS_COUGH = "HAS_COUGH"

        fun start(
            context: Context,
            hasTemperature: Boolean = false,
            hasCough: Boolean = false
        ) =
            context.startActivity(
                getIntent(
                    context,
                    hasTemperature,
                    hasCough
                )
            )

        private fun getIntent(
            context: Context,
            hasTemperature: Boolean,
            hasCough: Boolean
        ) =
            Intent(context, DiagnoseReviewActivity::class.java).apply {
                putExtra(HAS_COUGH, hasCough)
                putExtra(HAS_TEMPERATURE, hasTemperature)
            }
    }
}
