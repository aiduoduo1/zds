package com.jj.zds.data.dto.recheck

import com.jj.zds.data.dto.debugplan.DebugCheckGroupDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecheckSubmitRequestTest {

    @Test
    fun `draft and final requests keep their submission type`() {
        val group = DebugCheckGroupDto()

        val draft = RecheckSubmitRequest("SN-1", checkGroupDto = group, isDraft = true)
        val final = RecheckSubmitRequest("SN-1", checkGroupDto = group, isDraft = false)

        assertTrue(draft.isDraft)
        assertFalse(final.isDraft)
    }
}
