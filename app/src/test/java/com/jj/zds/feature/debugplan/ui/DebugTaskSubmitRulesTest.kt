package com.jj.zds.feature.debugplan.ui

import com.jj.zds.data.dto.debugplan.DebugCheckItemDto
import com.jj.zds.feature.debugplan.data.DebugTaskCheckResult
import com.jj.zds.feature.debugplan.data.DebugTaskSwitchRow
import com.jj.zds.feature.debugplan.data.DebugTaskTextRow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugTaskSubmitRulesTest {

    @Test
    fun `draft and submit use backend status values zero and one`() {
        assertEquals(0, DebugTaskSubmitMode.DRAFT.status)
        assertEquals(1, DebugTaskSubmitMode.SUBMIT.status)
    }

    @Test
    fun `text mismatch stays failed visually but does not require ng reporting`() {
        val textMismatch = DebugTaskTextRow(
            rowId = "text-row",
            checkId = "text-check",
            typeLabel = "1.1.1",
            title = "调试线",
            dto = DebugCheckItemDto(id = "text-check"),
            remark = "实际填写内容",
            expectedText = "提示内容",
            result = DebugTaskCheckResult.FAIL,
        )

        assertFalse(DebugTaskSubmitRules.hasReportableFailure(listOf(textMismatch)))
    }

    @Test
    fun `non text failure still requires ng reporting`() {
        val failedSwitch = DebugTaskSwitchRow(
            rowId = "switch-row",
            checkId = "switch-check",
            typeLabel = "1.1.2",
            title = "开关检查",
            dto = DebugCheckItemDto(id = "switch-check"),
            result = DebugTaskCheckResult.FAIL,
        )

        assertTrue(DebugTaskSubmitRules.hasReportableFailure(listOf(failedSwitch)))
    }

    @Test
    fun `reportable failure count excludes text mismatch`() {
        val textMismatch = DebugTaskTextRow(
            rowId = "text-row",
            checkId = "text-check",
            typeLabel = "1.1.1",
            title = "调试线",
            dto = DebugCheckItemDto(id = "text-check"),
            remark = "实际填写内容",
            expectedText = "提示内容",
            result = DebugTaskCheckResult.FAIL,
        )
        val failedSwitch = DebugTaskSwitchRow(
            rowId = "switch-row",
            checkId = "switch-check",
            typeLabel = "1.1.2",
            title = "开关检查",
            dto = DebugCheckItemDto(id = "switch-check"),
            result = DebugTaskCheckResult.FAIL,
        )

        assertEquals(
            1,
            DebugTaskSubmitRules.reportableFailureCount(listOf(textMismatch, failedSwitch)),
        )
    }
}
