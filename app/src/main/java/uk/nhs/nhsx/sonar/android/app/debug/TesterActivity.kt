/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_test.continue_button
import kotlinx.android.synthetic.main.activity_test.events
import kotlinx.android.synthetic.main.activity_test.exportButton
import kotlinx.android.synthetic.main.activity_test.no_events
import kotlinx.android.synthetic.main.activity_test.reset_button
import kotlinx.android.synthetic.main.activity_test.sonar_id
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.diagnose.SymptomsStateProvider
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.StatusStorage
import javax.inject.Inject

class TesterActivity : AppCompatActivity(R.layout.activity_test) {

    @Inject
    lateinit var statusStorage: StatusStorage

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var onboardingStatusProvider: OnboardingStatusProvider

    @Inject
    protected lateinit var symptomsStateProvider: SymptomsStateProvider

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<TestViewModel>

    private val viewModel: TestViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        sonar_id.text = "This is ${sonarIdProvider.getSonarId()}"
        val adapter = EventsAdapter()
        events.adapter = adapter
        events.layoutManager = LinearLayoutManager(this)

        continue_button.setOnClickListener {
            finish()
        }

        reset_button.setOnClickListener {
            statusStorage.clear()
            sonarIdProvider.clear()
            onboardingStatusProvider.clear()
            symptomsStateProvider.clear()
            viewModel.clear()
        }

        exportButton.setOnClickListener {
            viewModel.storeEvents(this)
        }

        viewModel.observeConnectionEvents().observe(this, Observer {
            Timber.d("Devices are $it")
            if (it.isEmpty()) no_events.visibility = View.VISIBLE
            else {
                no_events.visibility = View.GONE
                val ids = it.map { event -> event.id }.distinct()
                val unique = ids.map { id -> it.findLast { event -> event.id == id } }
                adapter.submitList(unique)
            }
        })

        viewModel.observeConnectionEvents()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, TesterActivity::class.java)
    }
}
