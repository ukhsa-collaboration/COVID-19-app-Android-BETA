/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
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
        stripUnderlines(explanationHow)

        explanationHow.movementMethod = LinkMovementMethod.getInstance()

        explanation_back.setOnClickListener {
            onBackPressed()
        }
    }

    private fun stripUnderlines(textView: TextView) {
        val s: Spannable = SpannableString(textView.text)
        val spans = s.getSpans(0, s.length, URLSpan::class.java)
        for (span in spans) {
            val start = s.getSpanStart(span)
            val end = s.getSpanEnd(span)
            s.removeSpan(span)
            val noUnderlineSpan = URLSpanNoUnderline(this, span.url)
            s.setSpan(noUnderlineSpan, start, end, 0)
        }
        textView.text = s
    }

    private class URLSpanNoUnderline(val context: Context, url: String) : URLSpan(url) {
        override fun updateDrawState(drawState: TextPaint) {
            super.updateDrawState(drawState)
            drawState.isUnderlineText = false
            drawState.color = ContextCompat.getColor(context, R.color.colorAccent)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ExplanationActivity::class.java)
    }
}
