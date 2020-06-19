/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.aboutUrl
import kotlinx.android.synthetic.main.activity_main.feelUnwellUrl
import kotlinx.android.synthetic.main.activity_main.uninstallUrl
import uk.nhs.nhsx.sonar.android.app.util.openUrl

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        feelUnwellUrl.setOnClickListener {
            openUrl("https://faq.covid19.nhs.uk/article/KA-01078/en-us")
        }

        uninstallUrl.setOnClickListener {
            openUrl("https://faq.covid19.nhs.uk/article/KA-01098/en-us")
        }

        aboutUrl.setOnClickListener {
            openUrl("https://faq.covid19.nhs.uk/article/KA-01097/en-us")
        }
    }

    companion object {

        fun getIntent(context: Context) =
            Intent(context, MainActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
