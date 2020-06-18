/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import uk.nhs.nhsx.sonar.android.app.R

class ParagraphsContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        orientation = VERTICAL
    }

    fun addAllParagraphs(vararg paragraphs: String) {
        removeAllViews()
        paragraphs.forEach { addParagraph(it) }
    }

    private fun addParagraph(text: String): ParagraphsContainer {
        val view = inflateTextView()
        view.text = text
        addView(view)
        return this
    }

    private fun inflateTextView(): TextView =
        (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.view_paragraph, this, false) as TextView
}

fun ParagraphsContainer.setRawText(rawText: String, separator: String = "\n\n") =
    addAllParagraphs(*rawText.split(separator).toTypedArray())
