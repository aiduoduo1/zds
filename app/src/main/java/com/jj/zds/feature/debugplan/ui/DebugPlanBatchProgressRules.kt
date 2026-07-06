package com.jj.zds.feature.debugplan.ui

import com.jj.zds.data.dto.debugplan.DebugPlanDto

object DebugPlanBatchProgressRules {

    fun subtitle(bindSnCount: Int, planCheckQty: Int): String {
        return "调试进度 ${bindSnCount.coerceAtLeast(0)}/${planCheckQty.coerceAtLeast(0)}"
    }

    fun selectPlan(plans: List<DebugPlanDto>, taskId: String): DebugPlanDto? {
        return plans.firstOrNull { it.id == taskId }
    }
}
