package com.jj.zds.feature.repair.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepairFileUrlBuilderTest {

    @Test
    fun buildRepairFileUrlReturnsNullWhenFileIdIsBlank() {
        assertNull(buildRepairFileUrl("", "token"))
    }

    @Test
    fun buildRepairFileUrlUsesDownloadEndpointAndToken() {
        val url = buildRepairFileUrl("12345", "abc")

        assertEquals("http://192.168.1.216:81/task/taskFile/download/12345?token=abc", url)
    }
}
