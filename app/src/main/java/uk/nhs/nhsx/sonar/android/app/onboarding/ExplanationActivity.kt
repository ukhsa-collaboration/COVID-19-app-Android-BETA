/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import kotlinx.android.synthetic.main.activity_explanation.explanationHow
import kotlinx.android.synthetic.main.activity_explanation.explanation_back
import uk.nhs.nhsx.sonar.android.app.R

class ExplanationActivity : AppCompatActivity(R.layout.activity_explanation) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        explanationHow.text = HtmlCompat.fromHtml(
            getString(R.string.explanation_how),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        explanationHow.movementMethod = LinkMovementMethod.getInstance()
        explanationHow.linksClickable = true
        ViewCompat.enableAccessibleClickableSpanSupport(explanationHow)

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
