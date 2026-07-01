package com.jj.zds.feature.debugplan.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugTaskNgLimitRulesTest {

    @Test
    fun `count below limit does not warn`() {
        assertFalse(DebugTaskNgLimitRules.shouldWarn(ngCount = 1, ngLimit = 2, sessionAcknowledged = false))
    }

    @Test
    fun `equal count and limit does not warn`() {
        assertFalse(DebugTaskNgLimitRules.shouldWarn(ngCount = 2, ngLimit = 2, sessionAcknowledged = false))
    }

    @Test
    fun `count above limit warns before session acknowledgement`() {
        assertTrue(DebugTaskNgLimitRules.shouldWarn(ngCount = 3, ngLimit = 2, sessionAcknowledged = false))
    }

    @Test
    fun `acknowledged session does not warn again`() {
        assertFalse(DebugTaskNgLimitRules.shouldWarn(ngCount = 3, ngLimit = 2, sessionAcknowledged = true))
    }

    @Test
    fun `contact supervisor log describes selection without claiming contact completed`() {
        val log = DebugTaskNgLimitRules.buildLog(
            action = DebugTaskNgLimitAction.CONTACT_SUPERVISOR,
            ngCount = 3,
            ngLimit = 2,
        )

        assertEquals("批次不良超限-联系主管", log.opName)
        assertEquals(
            "当前批次不良数为 3，已超过计划上限 2；操作员已知晓并选择联系主管，继续批次调试。",
            log.opDesc,
        )
    }

    @Test
    fun `continue work log records explicit continuation`() {
        val log = DebugTaskNgLimitRules.buildLog(
            action = DebugTaskNgLimitAction.CONTINUE_WORK,
            ngCount = 3,
            ngLimit = 2,
        )

        assertEquals("批次不良超限-继续工作", log.opName)
        assertEquals(
            "当前批次不良数为 3，已超过计划上限 2；操作员已知晓并选择继续工作。",
            log.opDesc,
        )
    }
}
