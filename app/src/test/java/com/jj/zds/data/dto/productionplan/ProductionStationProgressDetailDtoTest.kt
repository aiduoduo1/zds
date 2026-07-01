package com.jj.zds.data.dto.productionplan

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionStationProgressDetailDtoTest {

    @Test
    fun `station progress response keeps plan metadata and nested sta list`() {
        val detail = ProductionStationProgressDetailDto(
            planId = "261672096793554944",
            orderCode = "202606270001",
            partCode = "010903-016",
            partName = "托盘",
            staList = listOf(
                ProductionStationProgressDto(
                    operationType = 1,
                    finishedCount = 0,
                    totalCount = 10,
                ),
            ),
        )

        assertEquals("261672096793554944", detail.planId)
        assertEquals(1, detail.staList.size)
        assertEquals(10, detail.staList.first().totalCount)
    }
}
