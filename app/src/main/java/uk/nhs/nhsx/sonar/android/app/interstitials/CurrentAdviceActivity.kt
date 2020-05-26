/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.interstitials

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_current_advice.current_advice_desc
import kotlinx.android.synthetic.main.activity_current_advice.read_specific_advice
import kotlinx.android.synthetic.main.white_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.status.CheckinState
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.util.URL_LATEST_ADVICE_DEFAULT
import uk.nhs.nhsx.sonar.android.app.util.URL_LATEST_ADVICE_EXPOSED
import uk.nhs.nhsx.sonar.android.app.util.URL_LATEST_ADVICE_SYMPTOMATIC
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.setNavigateUpToolbar
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
import javax.inject.Inject

class CurrentAdviceActivity : AppCompatActivity(R.layout.activity_current_advice) {

    @Inject
    lateinit var userStateStorage: UserStateStorage

    private val state: UserState by lazy { userStateStorage.get() }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setNavigateUpToolbar(toolbar, title = R.string.read_current_advice)

        read_specific_advice.setOnClickListener {
            when (state) {
                DefaultState -> openUrl(URL_LATEST_ADVICE_DEFAULT)
                is ExposedState -> openUrl(URL_LATEST_ADVICE_EXPOSED)
                is SymptomaticState, is CheckinState, is PositiveState ->
                    openUrl(URL_LATEST_ADVICE_SYMPTOMATIC)
            }
        }

        current_advice_desc.text = state.until()?.let {
            getString(R.string.current_advice_desc_with_date, it.toUiFormat())
        } ?: getString(R.string.current_advice_desc_simple)
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, CurrentAdviceActivity::class.java)
    }
}
