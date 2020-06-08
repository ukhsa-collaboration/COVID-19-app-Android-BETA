/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.interstitials

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_apply_for_test.order_clinical_tests
import kotlinx.android.synthetic.main.activity_reference_code.reference_code_panel
import kotlinx.android.synthetic.main.white_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.BuildConfig
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.common.BaseActivity
import uk.nhs.nhsx.sonar.android.app.common.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCode
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeViewModel
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeViewModel.State.Loaded
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.setNavigateUpToolbar
import javax.inject.Inject

class ApplyForTestActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<ReferenceCodeViewModel>

    private val viewModel: ReferenceCodeViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_apply_for_test)
        setNavigateUpToolbar(toolbar, title = R.string.apply_for_test_title)

        viewModel.state().observe(this, Observer { state ->
            reference_code_panel.setState(state)

            order_clinical_tests.setOnClickListener {
                val url = when (state) {
                    is Loaded -> buildUrlWithCode(state.code)
                    else -> buildUrlWithoutCode()
                }
                openUrl(
                    url = url,
                    useInternalBrowser = false
                )
            }
        })

        viewModel.getReferenceCode()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ApplyForTestActivity::class.java)

        private fun buildUrlWithCode(code: ReferenceCode): String =
            "${BuildConfig.URL_APPLY_CORONAVIRUS_TEST}?ctaToken=${code.value}"

        private fun buildUrlWithoutCode(): String =
            BuildConfig.URL_APPLY_CORONAVIRUS_TEST
    }
}
