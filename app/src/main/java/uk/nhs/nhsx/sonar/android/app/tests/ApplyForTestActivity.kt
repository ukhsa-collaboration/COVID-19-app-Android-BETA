/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.tests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_apply_for_test.order_clinical_tests
import kotlinx.android.synthetic.main.activity_reference_code.reference_code_panel
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.white_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.BuildConfig
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCode
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeViewModel
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeViewModel.State.Loaded
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import javax.inject.Inject

class ApplyForTestActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<ReferenceCodeViewModel>

    private val viewModel: ReferenceCodeViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_apply_for_test)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_blue)
        supportActionBar?.setHomeActionContentDescription(R.string.go_back)
        supportActionBar?.title = getString(R.string.apply_for_test)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        viewModel.state().observe(this, Observer { state ->
            reference_code_panel.setState(state)

            order_clinical_tests.setOnClickListener {
                when (state) {
                    is Loaded -> openUrl(buildUrlWithCode(state.code))
                    else -> openUrl(buildUrlWithoutCode())
                }
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
