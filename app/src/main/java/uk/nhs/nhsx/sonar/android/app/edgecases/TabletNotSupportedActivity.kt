package uk.nhs.nhsx.sonar.android.app.edgecases

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.sonar.android.app.R

class TabletNotSupportedActivity : AppCompatActivity(R.layout.activity_tablet_not_supported) {

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) = Intent(context, TabletNotSupportedActivity::class.java)
    }
}
