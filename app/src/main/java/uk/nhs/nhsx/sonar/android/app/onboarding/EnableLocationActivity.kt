/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.paragraphContainer
import kotlinx.android.synthetic.main.activity_edge_case.takeActionButton
import kotlinx.android.synthetic.main.banner.toolbar_info
import uk.nhs.nhsx.sonar.android.app.ColorInversionAwareActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.LocationProviderChangedReceiver
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import javax.inject.Inject

open class EnableLocationActivity : ColorInversionAwareActivity(R.layout.activity_edge_case) {

    @Inject
    lateinit var locationHelper: LocationHelper

    private var locationSubscription: Disposable? = null
    private lateinit var locationProviderChangedReceiver: LocationProviderChangedReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        edgeCaseTitle.setText(R.string.enable_location_service_title)
        paragraphContainer.addAllParagraphs(
            getString(R.string.enable_location_service_rationale_p1),
            getString(R.string.enable_location_service_rationale_p2),
            getString(R.string.enable_location_service_rationale_p3),
            getString(R.string.enable_location_service_rationale_p4),
            getString(R.string.enable_location_service_rationale_p5)
        )

        toolbar_info.setOnClickListener {
            openUrl(URL_INFO)
        }

        takeActionButton.setText(R.string.go_to_your_settings)
        takeActionButton.setOnClickListener {
            startActivity(Intent(ACTION_LOCATION_SOURCE_SETTINGS))
        }

        locationProviderChangedReceiver = LocationProviderChangedReceiver(locationHelper)
    }

    override fun onResume() {
        super.onResume()
        if (locationHelper.isLocationEnabled()) {
            finish()
        }
        locationProviderChangedReceiver.register(this)

        locationSubscription =
            locationProviderChangedReceiver.getLocationStatus().subscribe { isLocationEnabled ->
                if (isLocationEnabled) {
                    finish()
                }
            }
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled) {
            takeActionButton.setBackgroundResource(R.drawable.button_round_background_inversed)
        } else {
            takeActionButton.setBackgroundResource(R.drawable.button_round_background)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(locationProviderChangedReceiver)
        locationSubscription?.dispose()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, EnableLocationActivity::class.java)
    }
}
