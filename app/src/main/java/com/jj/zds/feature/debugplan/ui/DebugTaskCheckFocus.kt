package com.jj.zds.feature.debugplan.ui

import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.jj.zds.R
import com.jj.zds.feature.debugplan.data.DebugTaskCheckRow
import com.jj.zds.feature.debugplan.data.DebugTaskValueRow

internal fun RecyclerView.focusDebugCheck(
    adapterPosition: Int,
    row: DebugTaskCheckRow,
    attempt: Int = 0,
) {
    scrollToPosition(adapterPosition)
    post {
        val itemView = findViewHolderForAdapterPosition(adapterPosition)?.itemView
        if (itemView == null) {
            if (attempt < 3) {
                postDelayed({ focusDebugCheck(adapterPosition, row, attempt + 1) }, 80L)
            } else {
                requestFocus()
            }
            return@post
        }
        if (row is DebugTaskValueRow) {
            val input = itemView.findViewById<EditText>(R.id.editValue)
            input?.requestFocus()
            input?.setSelection(input.text?.length ?: 0)
            context.getSystemService(InputMethodManager::class.java)
                ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        } else {
            itemView.isFocusableInTouchMode = true
            itemView.requestFocus()
        }
    }
}
