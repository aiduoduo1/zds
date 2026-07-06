package com.jj.zds.feature.debugplan.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugTaskInputRulesTest {

    @Test
    fun `explicit confirm type is not overridden by zero range`() {
        assertEquals(
            DebugTaskCollectType.CONFIRM,
            DebugTaskInputRules.resolveType("confirm", minValue = 0, maxValue = 0),
        )
    }

    @Test
    fun `num and input resolve to different collection types`() {
        assertEquals(
            DebugTaskCollectType.NUM,
            DebugTaskInputRules.resolveType("num", minValue = 10, maxValue = 50),
        )
        assertEquals(
            DebugTaskCollectType.INPUT,
            DebugTaskInputRules.resolveType("input", minValue = null, maxValue = null),
        )
    }

    @Test
    fun `num accepts at most eight integer digits and two decimal digits`() {
        assertTrue(DebugTaskInputRules.isValidNumCandidate("12345678.12"))
        assertTrue(DebugTaskInputRules.isValidNumCandidate("0.01"))
        assertFalse(DebugTaskInputRules.isValidNumCandidate("123456789"))
        assertFalse(DebugTaskInputRules.isValidNumCandidate("1.123"))
        assertFalse(DebugTaskInputRules.isValidNumCandidate("abc"))
    }

    @Test
    fun `input passes for any nonblank content without matching reference`() {
        assertEquals(50, DebugTaskInputRules.MAX_INPUT_LENGTH)
        assertTrue(DebugTaskInputRules.isInputMatched("任意调试说明", "标准参考值"))
        assertFalse(DebugTaskInputRules.isInputMatched("", "标准参考值"))
        assertFalse(DebugTaskInputRules.isInputMatched("   ", "标准参考值"))
    }
}
