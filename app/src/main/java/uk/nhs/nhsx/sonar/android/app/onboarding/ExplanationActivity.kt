/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_explanation.explanationMoreAboutTheApp
import kotlinx.android.synthetic.main.activity_explanation.explanationPrivacyNotice
import kotlinx.android.synthetic.main.activity_explanation.explanationTermsOfUse
import kotlinx.android.synthetic.main.activity_explanation.explanation_back
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.URL_PRIVACY_NOTICE
import uk.nhs.nhsx.sonar.android.app.util.URL_TERMS_OF_USE
import uk.nhs.nhsx.sonar.android.app.util.openUrl

class ExplanationActivity : AppCompatActivity(R.layout.activity_explanation) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        explanationMoreAboutTheApp.setOnClickListener {
            openUrl(URL_INFO)
        }

        explanationPrivacyNotice.setOnClickListener {
            openUrl(URL_PRIVACY_NOTICE)
        }

        explanationTermsOfUse.setOnClickListener {
            openUrl(URL_TERMS_OF_USE)
        }

        explanation_back.setOnClickListener {
            onBackPressed()
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ExplanationActivity::class.java)
    }
}
