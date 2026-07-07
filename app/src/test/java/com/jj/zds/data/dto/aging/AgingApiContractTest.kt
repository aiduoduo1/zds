package com.jj.zds.data.dto.aging

import com.jj.zds.data.api.ZdsApi
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.POST

class AgingApiContractTest {

    @Test
    fun `aging requests preserve backend field names`() {
        val product = AgingProductDto(id = "264865596292923392")

        assertEquals("PRO001", AgingScanValidRequest(qrCode = "PRO001").qrCode)
        assertEquals(product, AgingStartRequest(agingProList = listOf(product)).agingProList.first())
    }

    @Test
    fun `aging api paths match openapi contract`() {
        val scanMethod = ZdsApi::class.java.getMethod(
            "scanValidAgingProduct",
            AgingScanValidRequest::class.java,
            kotlin.coroutines.Continuation::class.java,
        )
        val startMethod = ZdsApi::class.java.getMethod(
            "startAging",
            AgingStartRequest::class.java,
            kotlin.coroutines.Continuation::class.java,
        )

        assertEquals(
            "/pro/proAgingPlan/beScanValid",
            scanMethod.getAnnotation(POST::class.java)?.value,
        )
        assertEquals(
            "/pro/proAgingPlan/startAging",
            startMethod.getAnnotation(POST::class.java)?.value,
        )

        val endScanMethod = ZdsApi::class.java.getMethod(
            "scanValidAgingEnd",
            AgingScanValidRequest::class.java,
            kotlin.coroutines.Continuation::class.java,
        )
        val endMethod = ZdsApi::class.java.getMethod(
            "endAging",
            AgingEndRequest::class.java,
            kotlin.coroutines.Continuation::class.java,
        )
        assertEquals(
            "/pro/proAgingPlan/afScanValid",
            endScanMethod.getAnnotation(POST::class.java)?.value,
        )
        assertEquals(
            "/pro/proAgingPlan/endAging",
            endMethod.getAnnotation(POST::class.java)?.value,
        )
    }

    @Test
    fun `end aging request carries scan record id`() {
        val request = AgingEndRequest(
            id = "264989806063452160",
            result = "NG",
            ngPhenomena = "功能失效",
        )

        assertEquals("264989806063452160", request.id)
        assertEquals("NG", request.result)
        assertEquals("功能失效", request.ngPhenomena)
    }
}
