package com.example.colocate.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.colocate.PermissionActivity
import com.example.colocate.R
import kotlinx.android.synthetic.main.activity_post_code.postCodeContinue

class PostCodeActivity : AppCompatActivity(R.layout.activity_post_code) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        postCodeContinue.setOnClickListener {
            PermissionActivity.start(this)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, PostCodeActivity::class.java)
    }
}
