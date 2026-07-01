package com.jj.zds.data.dto.debugplan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleDebugCheckRequestTest {

    @Test
    fun `draft and final requests keep their submission type`() {
        val group = DebugCheckGroupDto()

        val draft = SingleDebugCheckRequest("SN-1", checkGroupDto = group, isDraft = true)
        val final = SingleDebugCheckRequest("SN-1", checkGroupDto = group, isDraft = false)

        assertTrue(draft.isDraft)
        assertFalse(final.isDraft)
    }
}
