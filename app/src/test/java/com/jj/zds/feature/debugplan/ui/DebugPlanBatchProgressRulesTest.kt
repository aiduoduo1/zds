package com.jj.zds.feature.debugplan.ui

import com.jj.zds.data.dto.debugplan.DebugPlanDto
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugPlanBatchProgressRulesTest {

    @Test
    fun `subtitle shows bound sn count over plan quantity`() {
        assertEquals(
            "调试进度 30/100",
            DebugPlanBatchProgressRules.subtitle(bindSnCount = 30, planCheckQty = 100),
        )
    }

    @Test
    fun `subtitle prevents negative counts`() {
        assertEquals(
            "调试进度 0/0",
            DebugPlanBatchProgressRules.subtitle(bindSnCount = -1, planCheckQty = -1),
        )
    }

    @Test
    fun `latest progress selects current plan by task id`() {
        val current = DebugPlanDto(id = "task-2", bindSnCount = 40, planCheckQty = 100)

        assertEquals(
            current,
            DebugPlanBatchProgressRules.selectPlan(
                plans = listOf(DebugPlanDto(id = "task-1"), current),
                taskId = "task-2",
            ),
        )
    }
}
