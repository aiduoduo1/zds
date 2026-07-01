package com.jj.zds.feature.debugplan.ui

enum class DebugTaskNgLimitAction {
    CONTACT_SUPERVISOR,
    CONTINUE_WORK,
}

data class DebugTaskNgLimitLog(
    val opName: String,
    val opDesc: String,
)

object DebugTaskNgLimitRules {

    fun shouldWarn(
        ngCount: Int,
        ngLimit: Int,
        sessionAcknowledged: Boolean,
    ): Boolean {
        return !sessionAcknowledged && ngCount > ngLimit
    }

    fun buildLog(
        action: DebugTaskNgLimitAction,
        ngCount: Int,
        ngLimit: Int,
    ): DebugTaskNgLimitLog {
        return when (action) {
            DebugTaskNgLimitAction.CONTACT_SUPERVISOR -> DebugTaskNgLimitLog(
                opName = "批次不良超限-联系主管",
                opDesc = "当前批次不良数为 $ngCount，已超过计划上限 $ngLimit；操作员已知晓并选择联系主管，继续批次调试。",
            )

            DebugTaskNgLimitAction.CONTINUE_WORK -> DebugTaskNgLimitLog(
                opName = "批次不良超限-继续工作",
                opDesc = "当前批次不良数为 $ngCount，已超过计划上限 $ngLimit；操作员已知晓并选择继续工作。",
            )
        }
    }
}
