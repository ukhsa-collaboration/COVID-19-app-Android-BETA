package uk.nhs.nhsx.sonar.android.app.referencecode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.postDelayed
import kotlinx.android.synthetic.main.reference_code_panel.view.copy_content
import kotlinx.android.synthetic.main.reference_code_panel.view.copy_content_group
import kotlinx.android.synthetic.main.reference_code_panel.view.copy_content_label
import kotlinx.android.synthetic.main.reference_code_panel.view.divider
import kotlinx.android.synthetic.main.reference_code_panel.view.reference_code
import uk.nhs.nhsx.sonar.android.app.R

class ReferenceCodePanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.reference_code_panel, this)

        copy_content.setOnClickListener {
            if (!isCopyClicked) setCopyClickListener()
        }
    }

    var isError: Boolean = false
        set(value) = handleError(value)

    var isLoading: Boolean = false
        set(value) = handleLoading(value)

    private var isCopyClicked: Boolean = false

    fun setState(state: ReferenceCodeViewModel.State) {
        when (state) {
            ReferenceCodeViewModel.State.Loading -> isLoading = true
            ReferenceCodeViewModel.State.Error -> isError = true
            is ReferenceCodeViewModel.State.Loaded -> setRefCode(state.code.value)
        }
    }

    fun setRefCode(code: String) {
        reference_code.text = code
    }

    private fun handleError(value: Boolean) {
        if (value) setError() else reset()
    }

    private fun handleLoading(value: Boolean) {
        if (value) reset()
    }

    private fun setError() {
        reference_code.text = context.getString(R.string.error)
        divider.background = context.getDrawable(R.color.colorDanger)
        copy_content_group.visibility = View.INVISIBLE
    }

    private fun reset() {
        divider.background = context.getDrawable(R.color.colorPrimary)
        reference_code.text = ""
        copy_content_group.visibility = View.VISIBLE
    }

    private fun setCopyClickListener() {
        isCopyClicked = true

        getClipboardManager().apply {
            val clip = ClipData.newPlainText("Reference code", reference_code.text)
            setPrimaryClip(clip)
        }

        copy_content_label.visibility = View.VISIBLE

        postDelayed(COPIED_TEXT_DISPLAY_DURATION) {
            copy_content_label.visibility = View.INVISIBLE
            isCopyClicked = false
        }
    }

    private fun getClipboardManager(): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    companion object {
        private const val COPIED_TEXT_DISPLAY_DURATION = 3000L
    }
}
