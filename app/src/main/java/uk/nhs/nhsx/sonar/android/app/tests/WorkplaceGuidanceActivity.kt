package uk.nhs.nhsx.sonar.android.app.tests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.white_banner.toolbar
import uk.nhs.nhsx.sonar.android.app.R

class WorkplaceGuidanceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workplace_guidance)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_blue)
        supportActionBar?.setHomeActionContentDescription(R.string.go_back)
        supportActionBar?.title = getString(R.string.guidance_for_workplace)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, WorkplaceGuidanceActivity::class.java)
    }
}
