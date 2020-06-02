package uk.nhs.nhsx.sonar.android.app.referencecode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_reference_code.reference_code_panel
import kotlinx.android.synthetic.main.white_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
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
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ReferenceCodeActivity::class.java)
    }
}
