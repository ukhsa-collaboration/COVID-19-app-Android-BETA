/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_flow_test_start.start_main_activity
import org.jetbrains.annotations.TestOnly

@TestOnly
class FlowTestStartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        setContentView(R.layout.activity_flow_test_start)

        start_main_activity.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
