package com.jj.zds.feature.production.ui

import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindResultDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductionKeyPartBindRequestsTest {

    @Test
    fun `bind sends raw scan with operation type one`() {
        val request = ProductionKeyPartBindRequests.bind("plan", "pro", "RAW-SN-1")

        assertEquals("RAW-SN-1", request.matSn)
        assertNull(request.changeMatSn)
        assertEquals(1, request.operateType)
    }

    @Test
    fun `unbind sends old sn with operation type two`() {
        val request = ProductionKeyPartBindRequests.unbind("plan", "pro", "OLD-SN")

        assertEquals("OLD-SN", request.matSn)
        assertNull(request.changeMatSn)
        assertEquals(2, request.operateType)
    }

    @Test
    fun `replace sends new and selected old sn with operation type three`() {
        val request = ProductionKeyPartBindRequests.replace("plan", "pro", "NEW-SN", "OLD-SN")

        assertEquals("NEW-SN", request.matSn)
        assertEquals("OLD-SN", request.changeMatSn)
        assertEquals(3, request.operateType)
    }

    @Test
    fun `replacement candidates are trimmed filtered and unique`() {
        val result = ProductionKeyPartBindResultDto(
            boundMatSns = listOf(" OLD-1 ", "", "OLD-1", "OLD-2"),
        )

        assertEquals(
            listOf("OLD-1", "OLD-2"),
            ProductionKeyPartBindRequests.replacementCandidates(result),
        )
    }

    @Test
    fun `null replacement candidates from backend are treated as empty`() {
        val result = ProductionKeyPartBindResultDto()
        ProductionKeyPartBindResultDto::class.java
            .getDeclaredField("boundMatSns")
            .apply { isAccessible = true }
            .set(result, null)

        assertEquals(emptyList<String>(), ProductionKeyPartBindRequests.replacementCandidates(result))
    }
}
