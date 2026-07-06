package com.jj.zds.feature.debugplan.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DebugTaskItemDisplayRulesTest {

    @Test
    fun `photo and confirm display project content separately from description`() {
        val display = DebugTaskItemDisplayRules.build(
            standardOption = "确认接线牢固",
            description = "检查后选择结果",
        )

        assertEquals("确认接线牢固", display.content)
        assertEquals("检查后选择结果", display.description)
    }

    @Test
    fun `blank fields do not leave labels`() {
        val display = DebugTaskItemDisplayRules.build(
            standardOption = " ",
            description = null,
        )

        assertEquals("", display.content)
        assertEquals("", display.description)
    }

    @Test
    fun `long item name keeps five characters and adds ellipsis`() {
        assertEquals("电源模块装…", DebugTaskItemDisplayRules.shortTitle("电源模块装配确认"))
    }

    @Test
    fun `short item name stays unchanged`() {
        assertEquals("外观确认", DebugTaskItemDisplayRules.shortTitle("外观确认"))
    }
}
