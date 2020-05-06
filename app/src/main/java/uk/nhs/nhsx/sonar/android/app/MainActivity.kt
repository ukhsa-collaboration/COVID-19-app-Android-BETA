/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.confirm_onboarding
import kotlinx.android.synthetic.main.activity_main.explanation_link
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.sonar.android.app.edgecases.TabletNotSupportedActivity
import uk.nhs.nhsx.sonar.android.app.onboarding.ExplanationActivity
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeActivity
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.OkActivity
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import javax.inject.Inject

class MainActivity : ColorInversionAwareActivity() {

    @Inject
    lateinit var userStateStorage: UserStateStorage

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var onboardingStatusProvider: OnboardingStatusProvider

    @Inject
    lateinit var deviceDetection: DeviceDetection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        when {
            deviceDetection.isTablet() -> {
                finish()
                TabletNotSupportedActivity.start(this)
                return
            }
            deviceDetection.isUnsupported() -> {
                finish()
                DeviceNotSupportedActivity.start(this)
                return
            }
            sonarIdProvider.hasProperSonarId() -> {
                BluetoothService.start(this)
                navigateTo(userStateStorage.get())
                return
            }
            onboardingStatusProvider.get() -> {
                OkActivity.start(this)
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                return
            }
        }

        setContentView(R.layout.activity_main)

        confirm_onboarding.setOnClickListener {
            PostCodeActivity.start(this)
        }

        explanation_link.setOnClickListener {
            ExplanationActivity.start(this)
        }
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled) {
            confirm_onboarding.setBackgroundResource(R.drawable.button_round_background_inversed)
        } else {
            confirm_onboarding.setBackgroundResource(R.drawable.button_round_background)
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
