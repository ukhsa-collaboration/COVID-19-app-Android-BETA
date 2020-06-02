package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

class ParagraphsContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    fun addAllParagraphs(vararg paragraphs: String) {
        paragraphs.forEach { addParagraph(it) }
    }

    fun addParagraph(text: String): ParagraphsContainer {
        val view = inflateTextView()
        view.text = text
        addView(view)
        return this
    }

    private fun inflateTextView(): TextView =
        (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.view_paragraph, this, false) as TextView
}
