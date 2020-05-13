package uk.nhs.nhsx.sonar.android.app.referencecode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_reference_code.reference_code
import kotlinx.android.synthetic.main.activity_reference_code.reference_code_connect
import kotlinx.android.synthetic.main.activity_reference_code.reference_code_description_3
import kotlinx.android.synthetic.main.activity_reference_code.reference_code_divider
import kotlinx.android.synthetic.main.symptom_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import javax.inject.Inject

class ReferenceCodeActivity : AppCompatActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<ReferenceCodeViewModel>

    private val viewModel: ReferenceCodeViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        setContentView(R.layout.activity_reference_code)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_blue)
        supportActionBar?.setHomeActionContentDescription(R.string.go_back)
        supportActionBar?.title = getString(R.string.reference_code_title)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        reference_code_description_3.text = HtmlCompat.fromHtml(
            getString(R.string.reference_code_description_3),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        viewModel.state().observe(this, Observer { state ->
                when (state) {
                    ReferenceCodeViewModel.State.Loading -> {
                        reference_code_divider.background = getDrawable(R.color.colorPrimary)
                        reference_code_connect.isVisible = false
                        reference_code.isVisible = true
                        reference_code.text = getString(R.string.loading)
                    }
                    is ReferenceCodeViewModel.State.Loaded -> {
                        reference_code_divider.background = getDrawable(R.color.colorPrimary)
                        reference_code_connect.isVisible = false
                        reference_code.isVisible = true
                        reference_code.text = state.code.value
                    }
                    ReferenceCodeViewModel.State.Error -> {
                        reference_code_divider.background = getDrawable(R.color.colorDanger)
                        reference_code.isVisible = false
                        reference_code_connect.isVisible = true
                    }
                }
        })

        viewModel.getReferenceCode()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ReferenceCodeActivity::class.java)
    }
}
