package com.jj.zds.data.dto.debugplan

import org.junit.Assert.assertEquals
import org.junit.Test

class DebugTaskLogRequestTest {

    @Test
    fun `keeps exact log field names`() {
        val fieldNames = DebugTaskLogRequest::class.java.declaredFields
            .map { it.name }
            .toSet()

        assertEquals(setOf("taskId", "taskItemId", "opName", "opDesc"), fieldNames)
    }
}
