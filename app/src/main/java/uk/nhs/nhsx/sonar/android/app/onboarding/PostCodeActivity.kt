/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_post_code.invalidPostCodeHint
import kotlinx.android.synthetic.main.activity_post_code.postCodeContinue
import kotlinx.android.synthetic.main.activity_post_code.postCodeEditText
import kotlinx.android.synthetic.main.activity_post_code.postCodeRationale
import uk.nhs.nhsx.sonar.android.app.ColorInversionAwareActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.util.announce
import uk.nhs.nhsx.sonar.android.app.util.observeEvent
import javax.inject.Inject

class PostCodeActivity : ColorInversionAwareActivity(R.layout.activity_post_code) {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<PostCodeViewModel>

    private val viewModel: PostCodeViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        postCodeRationale.text =
            HtmlCompat.fromHtml(
                getString(R.string.post_code_rationale),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )

        postCodeContinue.setOnClickListener {
            viewModel.onContinue(postCodeEditText.text.toString())
        }

        viewModel.viewState().observe(this, Observer { viewState ->
            when (viewState) {
                PostCodeViewState.Valid -> {
                    postCodeEditText.setBackgroundResource(R.drawable.edit_text_background)
                    invalidPostCodeHint.isVisible = false
                }
                PostCodeViewState.Invalid -> {
                    postCodeEditText.setBackgroundResource(R.drawable.edit_text_background_error)
                    announce(R.string.valid_post_code_is_required)
                    invalidPostCodeHint.isVisible = true
                }
            }
        })

        viewModel.navigation().observeEvent(this, { navigation ->
            when (navigation) {
                PostCodeNavigation.Permissions -> PermissionActivity.start(this)
            }
        })
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
