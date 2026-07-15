package com.jj.zds.feature.productstockin

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductStockInRulesTest {

    @Test
    fun `merge removes invalid and duplicate pro codes`() {
        val existing = listOf(product("PRO-001", "PLAN-1"))
        val incoming = listOf(
            product("pro-001", "PLAN-1"),
            product("PRO-002", "PLAN-2"),
            product(" ", "PLAN-2"),
        )

        val result = ProductStockInRules.merge(existing, incoming)

        assertEquals(listOf("PRO-001", "PRO-002"), result.products.map { it.proCode })
        assertEquals(1, result.addedCount)
        assertEquals(1, result.duplicateCount)
        assertEquals(1, result.invalidCount)
    }

    @Test
    fun `confirm accepts passing plans and keeps blocked plan products`() {
        val products = listOf(
            product("PRO-001", "PLAN-PASS"),
            product("PRO-002", "PLAN-PASS"),
            product("PRO-003", "PLAN-BLOCK"),
        )
        val gates = listOf(
            ProductStockInPlanGate("PLAN-PASS", agingOkQty = 8, targetQty = 8),
            ProductStockInPlanGate("PLAN-BLOCK", agingOkQty = 3, targetQty = 5),
        )

        val result = ProductStockInRules.confirm(products, gates)

        assertEquals(listOf("PRO-001", "PRO-002"), result.accepted.map { it.proCode })
        assertEquals(listOf("PRO-003"), result.blocked.map { it.proCode })
        assertEquals(listOf("PLAN-BLOCK"), result.blockedPlans.map { it.planCode })
    }

    @Test
    fun `free aging plan passes without completed aging quantity`() {
        val product = product("PRO-FREE", "PLAN-FREE")
        val result = ProductStockInRules.confirm(
            products = listOf(product),
            gates = listOf(ProductStockInPlanGate("PLAN-FREE", agingOkQty = 0, targetQty = 0, agingFree = true)),
        )

        assertEquals(listOf(product), result.accepted)
        assertEquals(emptyList<ProductStockInProduct>(), result.blocked)
    }

    private fun product(proCode: String, planCode: String) = ProductStockInProduct(
        proCode = proCode,
        productName = "测试产品",
        partCode = "PART-001",
        planCode = planCode,
        source = "单个 PRO",
        gateLabel = "老化 OK",
    )
}
