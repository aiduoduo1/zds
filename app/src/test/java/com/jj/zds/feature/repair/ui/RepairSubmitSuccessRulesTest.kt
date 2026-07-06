package com.jj.zds.feature.repair.ui

import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairSubmitSuccessRulesTest {

    @Test
    fun onlyExactMatchedDictionaryLabelAllowsRecheck() {
        val items = listOf(
            DebugDictItemDto(value = "FIXED", label = "已修好，允许复调", isShow = true),
            DebugDictItemDto(value = "SCRAP", label = "报废", isShow = true),
        )

        assertTrue(RepairSubmitSuccessRules.isRecheckAllowed("FIXED", items))
        assertFalse(RepairSubmitSuccessRules.isRecheckAllowed("SCRAP", items))
        assertFalse(RepairSubmitSuccessRules.isRecheckAllowed("已修好，允许复调", items))
    }

    @Test
    fun fuzzyLabelDoesNotAllowRecheck() {
        val items = listOf(
            DebugDictItemDto(
                value = "FIXED_LATER",
                label = "已修好，允许复调（推荐）",
                isShow = true,
            ),
        )

        assertFalse(RepairSubmitSuccessRules.isRecheckAllowed("FIXED_LATER", items))
    }

    @Test
    fun handoffKeepsOriginalSnOrProQrCode() {
        assertEquals(
            "PRO||SN260625015||BOX-260625-001",
            RepairSubmitSuccessRules.handoffQrCode(
                "  PRO||SN260625015||BOX-260625-001  ",
            ),
        )
    }
}
