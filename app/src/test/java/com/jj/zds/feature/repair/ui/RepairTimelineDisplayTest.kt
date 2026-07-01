package com.jj.zds.feature.repair.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RepairTimelineDisplayTest {

    @Test
    fun parseSeparatesStatusSuffixFromContent() {
        val display = RepairTimelineDisplay.parse(
            "调试NG上报不良：电流值异常 状态：待维修",
        )

        assertEquals("调试NG上报不良：电流值异常", display.content)
        assertEquals("待维修", display.status)
    }

    @Test
    fun parseKeepsDescriptionWithoutStatusMarker() {
        val display = RepairTimelineDisplay.parse("更换异常器件并重新测试")

        assertEquals("更换异常器件并重新测试", display.content)
        assertEquals("", display.status)
    }

    @Test
    fun parseDoesNotRemoveBlankStatusMarker() {
        val display = RepairTimelineDisplay.parse("记录维修过程 状态：  ")

        assertEquals("记录维修过程 状态：", display.content)
        assertEquals("", display.status)
    }
}
