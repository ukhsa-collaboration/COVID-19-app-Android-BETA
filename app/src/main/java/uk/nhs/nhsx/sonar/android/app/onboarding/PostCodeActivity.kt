/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_post_code.*
import uk.nhs.nhsx.sonar.android.app.ColorInversionAwareActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.util.scrollToView
import javax.inject.Inject

class PostCodeActivity : ColorInversionAwareActivity(R.layout.activity_post_code) {

    @Inject
    lateinit var postCodeValidator: PostCodeValidator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        postCodeRationale.text =
            HtmlCompat.fromHtml(
                getString(R.string.post_code_rationale),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )

        postCodeContinue.setOnClickListener {
            val postCodeEntry = postCodeEditText.text.toString()

            if (postCodeValidator.validate(postCodeEntry)) {
                postCodeEditText.setBackgroundResource(R.drawable.edit_text_background)
                invalidPostCodeHint.isVisible = false
                PermissionActivity.start(this)
            } else {
                postCodeEditText.setBackgroundResource(R.drawable.edit_text_background_error)
                invalidPostCodeHint.announceForAccessibility(getString(R.string.valid_post_code_is_required))
                invalidPostCodeHint.isVisible = true
                scrollView.scrollToView(invalidPostCodeHint)
            }
        }
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled) {
            postCodeContinue.setBackgroundResource(R.drawable.button_round_background_inversed)
        } else {
            postCodeContinue.setBackgroundResource(R.drawable.button_round_background)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, PostCodeActivity::class.java)
    }
}
