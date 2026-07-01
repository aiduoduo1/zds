package com.jj.zds.feature.repair.ui

import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import org.junit.Assert.assertEquals
import org.junit.Test

class RepairResultDisplayTest {

    @Test
    fun `matching backend value displays dictionary label`() {
        val items = listOf(
            DebugDictItemDto(value = "REPAIRED", label = "维修完成"),
        )

        assertEquals("维修完成", RepairResultDisplay.labelFor("REPAIRED", items))
    }

    @Test
    fun `unmatched backend value falls back to original value`() {
        assertEquals("UNKNOWN", RepairResultDisplay.labelFor("UNKNOWN", emptyList()))
    }
}
