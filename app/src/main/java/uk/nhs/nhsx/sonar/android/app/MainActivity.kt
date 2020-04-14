/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.confirm_onboarding
import kotlinx.android.synthetic.main.activity_main.explanation_link
import uk.nhs.nhsx.sonar.android.app.ble.startBluetoothService
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeActivity
import uk.nhs.nhsx.sonar.android.app.persistence.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.persistence.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.OkActivity
import uk.nhs.nhsx.sonar.android.app.status.StatusStorage
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var statusStorage: StatusStorage

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var onboardingStatusProvider: OnboardingStatusProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_main)

        confirm_onboarding.setOnClickListener {
            PostCodeActivity.start(this)
        }

        explanation_link.setOnClickListener {
            ExplanationActivity.start(this)
        }

        if (sonarIdProvider.hasProperSonarId()) {
            startBluetoothService()
            navigateTo(statusStorage.get())
        } else if (onboardingStatusProvider.isOnboardingFinished()) {
            OkActivity.start(this)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        fun getIntent(context: Context) =
            Intent(context, MainActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
