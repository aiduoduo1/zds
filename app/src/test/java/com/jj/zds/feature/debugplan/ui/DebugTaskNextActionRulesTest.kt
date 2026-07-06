package com.jj.zds.feature.debugplan.ui

import com.jj.zds.data.dto.debugplan.DebugCheckItemDto
import com.jj.zds.feature.debugplan.data.DebugTaskCheckResult
import com.jj.zds.feature.debugplan.data.DebugTaskSwitchRow
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugTaskNextActionRulesTest {

    @Test
    fun `returns topmost required pending row`() {
        val rows = listOf(
            row("done", DebugTaskCheckResult.PASS),
            row("first-pending", DebugTaskCheckResult.PENDING),
            row("second-pending", DebugTaskCheckResult.PENDING),
        )

        assertEquals(1, DebugTaskNextActionRules.firstPendingIndex(rows))
    }

    @Test
    fun `ignores skippable pending row`() {
        val rows = listOf(
            row("skippable", DebugTaskCheckResult.PENDING, isSkipable = true),
            row("required", DebugTaskCheckResult.PENDING),
        )

        assertEquals(1, DebugTaskNextActionRules.firstPendingIndex(rows))
    }

    @Test
    fun `returns minus one when all required rows are handled`() {
        val rows = listOf(
            row("pass", DebugTaskCheckResult.PASS),
            row("fail", DebugTaskCheckResult.FAIL),
            row("skippable", DebugTaskCheckResult.PENDING, isSkipable = true),
        )

        assertEquals(-1, DebugTaskNextActionRules.firstPendingIndex(rows))
    }

    @Test
    fun `page handles enter key up only after check form is loaded`() {
        assertEquals(
            true,
            DebugTaskNextActionRules.shouldHandlePageEnter(
                hasCheckForm = true,
                isEnterKey = true,
                isKeyUp = true,
            ),
        )
        assertEquals(
            false,
            DebugTaskNextActionRules.shouldHandlePageEnter(
                hasCheckForm = false,
                isEnterKey = true,
                isKeyUp = true,
            ),
        )
        assertEquals(
            false,
            DebugTaskNextActionRules.shouldHandlePageEnter(
                hasCheckForm = true,
                isEnterKey = true,
                isKeyUp = false,
            ),
        )
    }

    private fun row(
        id: String,
        result: DebugTaskCheckResult,
        isSkipable: Boolean = false,
    ): DebugTaskSwitchRow {
        return DebugTaskSwitchRow(
            rowId = id,
            checkId = id,
            typeLabel = id,
            title = id,
            dto = DebugCheckItemDto(id = id, isSkipable = isSkipable),
            result = result,
        )
    }
}
