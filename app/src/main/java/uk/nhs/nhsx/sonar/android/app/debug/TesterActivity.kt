/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.debug

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_test.*
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramProvider
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramStorage
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.util.observe
import javax.inject.Inject

fun cryptogramColourAndInverse(cryptogramBytes: ByteArray): Pair<Int, Int> {
    val r = cryptogramBytes[0].toInt()
    val g = cryptogramBytes[1].toInt()
    val b = cryptogramBytes[2].toInt()
    return Pair(Color.rgb(r, g, b), Color.rgb(255 - r, 255 - g, 255 - b))
}

class TesterActivity : AppCompatActivity(R.layout.activity_test) {

    @Inject
    lateinit var userStateStorage: UserStateStorage

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var cryptogramStorage: CryptogramStorage

    @Inject
    lateinit var cryptogramProvider: CryptogramProvider

    @Inject
    lateinit var activationCodeProvider: ActivationCodeProvider

    @Inject
    lateinit var onboardingStatusProvider: OnboardingStatusProvider

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<TestViewModel>

    private val viewModel: TestViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        sonar_id.text = sonarIdProvider.get()
        if (cryptogramProvider.canProvideCryptogram()) {
            val cryptogramBytes = cryptogramProvider.provideCryptogram().asBytes()
            val (cryptogramColour, inverseColour) = cryptogramColourAndInverse(cryptogramBytes)
            encrypted_broadcast_id.text = Base64.encodeToString(cryptogramBytes, Base64.DEFAULT)
            encrypted_broadcast_id.setBackgroundColor(cryptogramColour)
            encrypted_broadcast_id.setTextColor(inverseColour)
        } else {
            encrypted_broadcast_id.text = "Cannot generate cryptogram"
        }

        val adapter = EventsAdapter()
        events.adapter = adapter
        events.layoutManager = LinearLayoutManager(this)

        continue_button.setOnClickListener {
            finish()
        }

        reset_button.setOnClickListener {
            userStateStorage.clear()
            sonarIdProvider.clear()
            onboardingStatusProvider.clear()
            activationCodeProvider.clear()
            cryptogramStorage.clear()
            viewModel.clear()
        }

        exportButton.setOnClickListener {
            viewModel.storeEvents(this)
        }

        viewModel.observeConnectionEvents().observe(this) {
            Timber.d("Devices are $it")
            if (it.isEmpty()) no_events.visibility = View.VISIBLE
            else {
                no_events.visibility = View.GONE
                val ids = it.map { event -> event.cryptogram }.distinct()
                val unique = ids.map { id -> it.findLast { event -> event.cryptogram == id } }
                adapter.submitList(unique)
            }
        }

        viewModel.observeConnectionEvents()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, TesterActivity::class.java)
    }
}
