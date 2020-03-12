package com.example.colocate

import android.os.Bundle
import android.text.Html
import android.text.Html.FROM_HTML_MODE_COMPACT
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val link = this.findViewById<TextView>(R.id.explanation_link)
        link.text = Html.fromHtml(getString(R.string.explanation_link), FROM_HTML_MODE_COMPACT)
        link.movementMethod = LinkMovementMethod.getInstance()
    }
}
