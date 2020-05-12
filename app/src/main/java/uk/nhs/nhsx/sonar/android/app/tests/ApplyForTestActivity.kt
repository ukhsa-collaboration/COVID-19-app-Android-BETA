/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.tests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_apply_for_test.order_clinical_tests
import kotlinx.android.synthetic.main.symptom_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.URL_APPLY_CORONAVIRUS_TEST
import uk.nhs.nhsx.sonar.android.app.util.openUrl

class ApplyForTestActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_apply_for_test)

        toolbar.setTitle(R.string.get_tested)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        order_clinical_tests.setOnClickListener {
            openUrl(URL_APPLY_CORONAVIRUS_TEST)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ApplyForTestActivity::class.java)
    }
}
