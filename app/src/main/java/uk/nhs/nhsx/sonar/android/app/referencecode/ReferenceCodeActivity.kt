/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_reference_code.reference_code_panel
import kotlinx.android.synthetic.main.activity_reference_code.scrollView
import kotlinx.android.synthetic.main.activity_reference_code.testResultMeaningTitle
import kotlinx.android.synthetic.main.activity_reference_code.testResultMeaningUrl
import kotlinx.android.synthetic.main.white_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.common.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.util.URL_TEST_RESULT_MEANING
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.scrollToView
import uk.nhs.nhsx.sonar.android.app.util.setNavigateUpToolbar
import javax.inject.Inject

class ReferenceCodeActivity : AppCompatActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<ReferenceCodeViewModel>

    private val viewModel: ReferenceCodeViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        setContentView(R.layout.activity_reference_code)

        setNavigateUpToolbar(toolbar, title = R.string.reference_code_title)

        viewModel.state().observe(this, Observer { state ->
            reference_code_panel.setState(state)
        })

        viewModel.getReferenceCode()

        testResultMeaningUrl.setOnClickListener {
            openUrl(URL_TEST_RESULT_MEANING)
        }

        if (shouldFocusOnTestResultMeaning()) {
            scrollAndAnnounce()
        }
    }

    private fun shouldFocusOnTestResultMeaning(): Boolean =
        intent.getBooleanExtra(FOCUS_TEST_RESULT_MEANING_KEY, false)

    private fun scrollAndAnnounce() {
        val rootView = findViewById<View>(android.R.id.content).rootView
        scrollView.scrollToView(testResultMeaningTitle)
        OneTimeAccessibilityFocusRunner(rootView) {
            testResultMeaningTitle.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }

    companion object {
        private const val FOCUS_TEST_RESULT_MEANING_KEY = "FOCUS_TEST_RESULT_MEANING_KEY"

        fun start(context: Context) =
            context.startActivity(getIntent(context))

        fun startWithFocusOnTestResultMeaning(context: Context) =
            context.startActivity(intentWithFocusKey(context))

        fun intentWithFocusKey(context: Context) =
            getIntent(context).apply { putExtra(FOCUS_TEST_RESULT_MEANING_KEY, true) }

        private fun getIntent(context: Context) =
            Intent(context, ReferenceCodeActivity::class.java)
    }

    private class OneTimeAccessibilityFocusRunner(
        private val rootView: View,
        private val runnableFn: () -> Unit
    ) : View.AccessibilityDelegate() {

        init {
            rootView.accessibilityDelegate = this
        }

        override fun onRequestSendAccessibilityEvent(
            @Nullable host: ViewGroup?,
            @Nullable child: View?,
            @Nullable event: AccessibilityEvent?
        ): Boolean {
            runAccessibilityEventMaybe(event)
            return super.onRequestSendAccessibilityEvent(host, child, event)
        }

        private fun runAccessibilityEventMaybe(event: AccessibilityEvent?) {
            if (isAccessibilityFocusEvent(event)) {
                rootView.accessibilityDelegate = null
                runnableFn()
            }
        }

        private fun isAccessibilityFocusEvent(event: AccessibilityEvent?): Boolean =
            event?.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
    }
}
