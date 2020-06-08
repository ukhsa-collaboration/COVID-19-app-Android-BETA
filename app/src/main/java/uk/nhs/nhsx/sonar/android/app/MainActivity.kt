/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.common.ColorInversionAwareActivity
import uk.nhs.nhsx.sonar.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.sonar.android.app.edgecases.TabletNotSupportedActivity
import uk.nhs.nhsx.sonar.android.app.onboarding.MainOnboardingActivity
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.startStatusActivity
import uk.nhs.nhsx.sonar.android.app.util.DeviceDetection
import javax.inject.Inject

class MainActivity : ColorInversionAwareActivity() {

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
            }
            deviceDetection.isUnsupported() -> {
                finish()
                DeviceNotSupportedActivity.start(this)
            }
            sonarIdProvider.hasProperSonarId() -> {
                BluetoothService.start(this)
                startStatusActivity()
            }
            onboardingStatusProvider.get() -> {
                startStatusActivity()
            }
            else -> {
                finish()
                MainOnboardingActivity.start(this)
            }
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
