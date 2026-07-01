package com.jj.zds.data.dto.debugplan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugSnCheckRequestTest {

    @Test
    fun `draft and final requests keep their submission type`() {
        val group = DebugCheckGroupDto()

        val draft = DebugSnCheckRequest("task", "sn", checkGroupDto = group, isDraft = true)
        val final = DebugSnCheckRequest("task", "sn", checkGroupDto = group, isDraft = false)

        assertTrue(draft.isDraft)
        assertFalse(final.isDraft)
    }
}
