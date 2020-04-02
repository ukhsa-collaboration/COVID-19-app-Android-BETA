package com.example.colocate

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.colocate.persistence.SharedPreferencesResidentIdProvider
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.SharedPreferencesStatusStorage
import com.example.colocate.status.StatusStorage
import kotlinx.android.synthetic.mock.activity_test.continue_button
import kotlinx.android.synthetic.mock.activity_test.events
import kotlinx.android.synthetic.mock.activity_test.no_events
import kotlinx.android.synthetic.mock.activity_test.reset_button
import timber.log.Timber

class TestActivity : AppCompatActivity() {

    private val statusStorage: StatusStorage by lazy {
        SharedPreferencesStatusStorage(this)
    }

    private val residentIdProvider: SharedPreferencesResidentIdProvider by lazy {
        SharedPreferencesResidentIdProvider(this)
    }

    private lateinit var viewModelFactory: TestViewModelFactory

    private val viewModel: TestViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val adapter = EventsAdapter()
        events.adapter = adapter
        events.layoutManager = LinearLayoutManager(this)

        viewModelFactory = TestViewModelFactory(
            (applicationContext as ColocateApplication).appComponent.provideEventsV2Dao()
        )

        continue_button.setOnClickListener {
            navigateToMain()
        }

        reset_button.setOnClickListener {
            statusStorage.update(CovidStatus.OK)

            residentIdProvider.clear()

            viewModel.clear()

            navigateToMain()
        }

        viewModel.observeEvents().observe(this, Observer {
            Timber.d("<<<< events are $it")
            if (it.isEmpty()) {
                no_events.visibility = View.VISIBLE
            } else {
                no_events.visibility = View.GONE
                adapter.submitList(it)
            }
        })

        viewModel.getEvents()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
