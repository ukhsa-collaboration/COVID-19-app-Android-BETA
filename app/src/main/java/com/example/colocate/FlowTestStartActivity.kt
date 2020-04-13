/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.colocate.persistence.AppDatabase
import com.example.colocate.persistence.SonarIdProvider
import com.example.colocate.status.StatusStorage
import org.jetbrains.annotations.TestOnly
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage
import javax.inject.Inject

@TestOnly
class FlowTestStartActivity : AppCompatActivity() {

    @Inject
    lateinit var statusStorage: StatusStorage

    @Inject
    lateinit var encryptionKeyStorage: EncryptionKeyStorage

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        setContentView(R.layout.activity_flow_test_start)

        findViewById<AppCompatButton>(R.id.start_main_activity).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
