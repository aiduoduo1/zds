package com.jj.zds.data.dto.productstockout

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductStockOutScanRequestTest {

    @Test
    fun `scan request keeps its child task and scanned code`() {
        val request = ProductStockOutScanRequest(
            dtlId = "delivery-detail-id",
            qrCode = "PRO20260729000002",
        )

        assertEquals("delivery-detail-id", request.dtlId)
        assertEquals("PRO20260729000002", request.qrCode)
    }
}
