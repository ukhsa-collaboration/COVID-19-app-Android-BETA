/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_review_diagnosis.submission_error
import kotlinx.android.synthetic.main.symptom_banner.close_btn
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.showToast
import uk.nhs.nhsx.sonar.android.app.status.CovidStatus
import uk.nhs.nhsx.sonar.android.app.status.IsolateActivity
import uk.nhs.nhsx.sonar.android.app.status.OkActivity
import uk.nhs.nhsx.sonar.android.app.status.StatusStorage
import javax.inject.Inject

class DiagnoseReviewActivity : BaseActivity() {
    @Inject
    protected lateinit var statusStorage: StatusStorage

    @Inject
    protected lateinit var viewModelFactory: ViewModelFactory<DiagnoseReviewViewModel>

    private val viewModel: DiagnoseReviewViewModel by viewModels {
        viewModelFactory
    }

    private val hasTemperature: Boolean by lazy {
        intent.getBooleanExtra(HAS_TEMPERATURE, false)
    }

    private val hasCough: Boolean by lazy {
        intent.getBooleanExtra(HAS_COUGH, false)
    }

    private val confirmButton: Button by lazy {
        findViewById<Button>(R.id.confirm_diagnosis)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_review_diagnosis)

        close_btn.setImageDrawable(getDrawable(R.drawable.ic_arrow_back))
        close_btn.setOnClickListener {
            onBackPressed()
        }

        viewModel.isolationResult.observe(this, Observer { result ->
            if (result is ViewState.Success) {
                viewModel.clearContactEvents()

                showToast(R.string.successfull_data_upload)

                updateStatusAndNavigate()
                submission_error.visibility = View.GONE
            } else {
                submission_error.visibility = View.VISIBLE
                confirmButton.text = getString(R.string.retry)
            }
        })

        confirmButton.setOnClickListener {
            if (hasCough || hasTemperature) {
                viewModel.uploadContactEvents()
            } else {
                updateStatusAndNavigate()
            }
        }
    }

    private fun updateStatusAndNavigate() {
        if (hasCough or hasTemperature) {
            statusStorage.update(CovidStatus.RED)
            IsolateActivity.start(this)
        } else {
            statusStorage.update(CovidStatus.OK)
            OkActivity.start(this)
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
