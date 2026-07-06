package com.jj.zds.feature.debugplan.ui

import com.jj.zds.feature.debugplan.data.DebugTaskCheckResult
import com.jj.zds.feature.debugplan.data.DebugTaskCheckRow

internal object DebugTaskNextActionRules {

    fun firstPendingIndex(rows: List<DebugTaskCheckRow>): Int {
        return rows.indexOfFirst {
            it.result == DebugTaskCheckResult.PENDING && !it.dto.isSkipable
        }
    }

    fun shouldHandlePageEnter(
        hasCheckForm: Boolean,
        isEnterKey: Boolean,
        isKeyUp: Boolean,
    ): Boolean {
        return hasCheckForm && isEnterKey && isKeyUp
    }
}
