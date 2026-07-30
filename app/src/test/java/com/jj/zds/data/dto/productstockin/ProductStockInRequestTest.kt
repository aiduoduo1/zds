package com.jj.zds.data.dto.productstockin

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductStockInRequestTest {

    @Test
    fun `request keeps the scanned flow ticket or product code in qrCode`() {
        assertEquals("FC20260729000002", ProductStockInRequest("FC20260729000002").qrCode)
    }
}
