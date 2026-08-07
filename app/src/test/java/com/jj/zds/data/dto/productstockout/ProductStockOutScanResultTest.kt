package com.jj.zds.data.dto.productstockout

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductStockOutScanResultTest {

    @Test
    fun `scan result keeps successful and blocked product codes in separate groups`() {
        val result = ProductStockOutScanResult(
            groups = mapOf(
                "已出库" to listOf("PRO0001"),
                "未入库" to listOf("PRO0002", "PRO0003"),
            ),
        )

        assertEquals(listOf("PRO0001"), result.groups["已出库"])
        assertEquals(listOf("PRO0002", "PRO0003"), result.groups["未入库"])
    }
}
