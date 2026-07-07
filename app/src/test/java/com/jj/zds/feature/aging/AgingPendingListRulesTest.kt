package com.jj.zds.feature.aging

import com.jj.zds.data.dto.aging.AgingProductDto
import org.junit.Assert.assertEquals
import org.junit.Test

class AgingPendingListRulesTest {

    @Test
    fun `merge keeps each pro code only once across existing and scanned products`() {
        val existing = listOf(product("PRO-001", partCode = "PART-A", planId = "PLAN-1"))
        val incoming = listOf(
            product("PRO-001", partCode = "PART-A", planId = "PLAN-1"),
            product("PRO-002", partCode = "PART-B", planId = "PLAN-2"),
            product("PRO-002", partCode = "PART-B", planId = "PLAN-2"),
        )

        val result = AgingPendingListRules.merge(existing, incoming)

        assertEquals(listOf("PRO-001", "PRO-002"), result.products.map { it.materialCode })
        assertEquals(1, result.addedCount)
        assertEquals(2, result.duplicateCount)
    }

    @Test
    fun `merge rejects products without a pro code`() {
        val result = AgingPendingListRules.merge(
            existing = emptyList(),
            incoming = listOf(AgingProductDto(materialCode = null), AgingProductDto(materialCode = " ")),
        )

        assertEquals(emptyList<AgingProductDto>(), result.products)
        assertEquals(2, result.invalidCount)
    }

    @Test
    fun `summary counts products parts and plans for button caption`() {
        val products = listOf(
            product("PRO-001", partCode = "PART-A", planId = "PLAN-1"),
            product("PRO-002", partCode = "PART-A", planId = "PLAN-1"),
            product("PRO-003", partCode = "PART-B", planId = null, planOrderCode = "ORDER-2"),
        )

        assertEquals(
            "共 3 台 · 2 种产品 · 2 个生产计划",
            AgingPendingListRules.summary(products),
        )
    }

    private fun product(
        materialCode: String,
        partCode: String,
        planId: String?,
        planOrderCode: String? = null,
    ): AgingProductDto {
        return AgingProductDto(
            materialCode = materialCode,
            partCode = partCode,
            planId = planId,
            planOrderCode = planOrderCode,
        )
    }
}
