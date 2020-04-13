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
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BleEvents
import uk.nhs.nhsx.sonar.android.app.persistence.SharedPreferencesSonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.CovidStatus
import uk.nhs.nhsx.sonar.android.app.status.SharedPreferencesStatusStorage
import uk.nhs.nhsx.sonar.android.app.status.StatusStorage

class TesterActivity : AppCompatActivity(R.layout.activity_test) {

    private val statusStorage: StatusStorage by lazy {
        SharedPreferencesStatusStorage(this)
    }

    private val sonarIdProvider: SharedPreferencesSonarIdProvider by lazy {
        SharedPreferencesSonarIdProvider(this)
    }

    private lateinit var viewModelFactory: TestViewModelFactory

    private val viewModel: TestViewModel by viewModels { viewModelFactory }

    private val bleEvents: BleEvents by lazy {
        appComponent.provideBleEvents()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sonar_id.text = "This is ${sonarIdProvider.getSonarId()}"
        val adapter = EventsAdapter()
        events.adapter = adapter
        events.layoutManager = LinearLayoutManager(this)

        viewModelFactory = TestViewModelFactory(
            this,
            appComponent.provideEventsV2Dao(),
            bleEvents
        )

        continue_button.setOnClickListener {
            finish()
        }

        reset_button.setOnClickListener {
            statusStorage.update(CovidStatus.OK)

            sonarIdProvider.clear()

            viewModel.clear()
        }

        exportButton.setOnClickListener {
            viewModel.storeEvents()
        }

        viewModel.observeConnectionEvents().observe(this, Observer {
            Timber.d("<<<< devices are $it")
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
