package com.jj.zds.feature.productstockin

object ProductStockInRules {

    fun merge(
        existing: List<ProductStockInProduct>,
        incoming: List<ProductStockInProduct>,
    ): ProductStockInMergeResult {
        val products = existing.toMutableList()
        val knownCodes = existing.mapTo(mutableSetOf()) { it.proCode.trim().uppercase() }
        var addedCount = 0
        var duplicateCount = 0
        var invalidCount = 0

        incoming.forEach { product ->
            val code = product.proCode.trim()
            when {
                code.isBlank() -> invalidCount++
                !knownCodes.add(code.uppercase()) -> duplicateCount++
                else -> {
                    products += product.copy(proCode = code)
                    addedCount++
                }
            }
        }
        return ProductStockInMergeResult(products, addedCount, duplicateCount, invalidCount)
    }

    fun confirm(
        products: List<ProductStockInProduct>,
        gates: List<ProductStockInPlanGate>,
    ): ProductStockInConfirmResult {
        val gateByPlan = gates.associateBy { it.planCode }
        val blockedPlans = products
            .map { it.planCode }
            .distinct()
            .mapNotNull(gateByPlan::get)
            .filterNot { it.agingFree || it.agingOkQty >= it.targetQty }
        val blockedCodes = blockedPlans.mapTo(mutableSetOf()) { it.planCode }
        return ProductStockInConfirmResult(
            accepted = products.filterNot { it.planCode in blockedCodes },
            blocked = products.filter { it.planCode in blockedCodes },
            blockedPlans = blockedPlans,
        )
    }
}
