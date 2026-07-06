package com.jj.zds.feature.debugplan.ui

import android.content.Context
import android.text.InputFilter
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.lifecycle.LifecycleOwner
import com.jj.ui.dialog.JBaseDialog
import com.jj.ui.dialog.JInputDialog

internal class DebugTaskRemarkDialog(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    private val onNext: (String) -> Unit,
) : JInputDialog(context, lifecycleOwner) {

    init {
        (inputLayout.focusedInputView() as? EditText)?.filters = arrayOf(
            InputFilter.LengthFilter(DebugTaskInputRules.MAX_INPUT_LENGTH),
        )
    }

    override fun show(): JBaseDialog {
        val dialog = super.show()
        (inputLayout.focusedInputView() as? EditText)?.apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setOnEditorActionListener { _, actionId, event ->
                val isNext = actionId == EditorInfo.IME_ACTION_NEXT
                val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                if (isNext || isEnter) {
                    onNext(inputLayout.text())
                    dialog.dismiss()
                    true
                } else {
                    false
                }
            }
        }
        return dialog
    }
}
