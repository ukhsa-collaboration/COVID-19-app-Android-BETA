/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_review_close.close_review_btn
import kotlinx.android.synthetic.main.activity_review_close.nhs_service
import kotlinx.android.synthetic.main.white_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.common.BaseActivity
import uk.nhs.nhsx.sonar.android.app.status.startStatusActivity
import uk.nhs.nhsx.sonar.android.app.util.URL_SYMPTOM_CHECKER
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.setNavigateUpToolbar

class DiagnoseCloseActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_review_close)

        setNavigateUpToolbar(toolbar, R.string.add_my_symptoms)

        close_review_btn.setOnClickListener {
            startStatusActivity()
        }

        nhs_service.setOnClickListener {
            openUrl(URL_SYMPTOM_CHECKER)
        }
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled) {
            close_review_btn.setBackgroundResource(R.drawable.button_round_background_inversed)
        } else {
            close_review_btn.setBackgroundResource(R.drawable.button_round_background)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, DiagnoseCloseActivity::class.java)
    }
}
