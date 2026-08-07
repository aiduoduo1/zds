package com.jj.zds.feature.productstockout

import com.jj.zds.data.dto.productstockout.ProductStockOutChildTaskDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductStockOutTaskFinishRulesTest {

    @Test
    fun `can finish only when every child task is completed`() {
        assertTrue(
            ProductStockOutTaskFinishRules.canFinish(
                listOf(
                    ProductStockOutChildTaskDto(status = 2),
                    ProductStockOutChildTaskDto(statusLabel = "已完成"),
                ),
            ),
        )
        assertFalse(
            ProductStockOutTaskFinishRules.canFinish(
                listOf(
                    ProductStockOutChildTaskDto(status = 2),
                    ProductStockOutChildTaskDto(statusLabel = "出库中"),
                ),
            ),
        )
    }

    @Test
    fun `empty child task list cannot finish`() {
        assertFalse(ProductStockOutTaskFinishRules.canFinish(emptyList()))
    }
}
