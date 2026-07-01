package com.jj.zds.feature.repair.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RepairRemarkDisplayTest {

    @Test
    fun `chinese and english commas are rendered as trimmed lines`() {
        assertEquals(
            "第一段\n第二段\n第三段",
            RepairRemarkDisplay.format("第一段， 第二段,第三段，，,"),
        )
    }

    @Test
    fun `text without commas stays unchanged`() {
        assertEquals("单段说明", RepairRemarkDisplay.format("单段说明"))
    }

    @Test
    fun `at signs are hidden and rendered as line breaks`() {
        assertEquals(
            "变压器二次对地绝缘【10 ~ 50】,实际数值:1\n" +
                "变压器一次对地绝缘【500 ~ 1000】,实际数值:1\n" +
                "变压器前接线板异常\n" +
                "检查外观异常",
            RepairRemarkDisplay.format(
                "变压器二次对地绝缘【10 ~ 50】,实际数值:1@" +
                    "变压器一次对地绝缘【500 ~ 1000】,实际数值:1@" +
                    "变压器前接线板异常@检查外观异常",
            ),
        )
    }
}
