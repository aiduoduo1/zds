package com.jj.zds.feature.debugplan.ui

import com.jj.zds.feature.debugplan.data.DebugTaskCheckResult
import com.jj.zds.feature.debugplan.data.DebugTaskCheckRow
import com.jj.zds.feature.debugplan.data.DebugTaskTextRow

enum class DebugTaskSubmitMode(val status: Int) {
    DRAFT(status = 0),
    SUBMIT(status = 1),
}

object DebugTaskSubmitRules {

    fun hasReportableFailure(rows: List<DebugTaskCheckRow>): Boolean {
        return reportableFailureCount(rows) > 0
    }

    fun reportableFailureCount(rows: List<DebugTaskCheckRow>): Int {
        return rows.count { row ->
            row.result == DebugTaskCheckResult.FAIL && row !is DebugTaskTextRow
        }
    }
}
