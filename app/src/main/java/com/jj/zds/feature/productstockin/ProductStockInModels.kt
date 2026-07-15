package com.jj.zds.feature.productstockin

data class ProductStockInProduct(
    val proCode: String,
    val productName: String,
    val partCode: String,
    val planCode: String,
    val source: String,
    val gateLabel: String,
)

data class ProductStockInPlanGate(
    val planCode: String,
    val agingOkQty: Int,
    val targetQty: Int,
    val agingFree: Boolean = false,
)

data class ProductStockInMergeResult(
    val products: List<ProductStockInProduct>,
    val addedCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
)

data class ProductStockInConfirmResult(
    val accepted: List<ProductStockInProduct>,
    val blocked: List<ProductStockInProduct>,
    val blockedPlans: List<ProductStockInPlanGate>,
)

sealed interface ProductStockInScanResult {
    data class Accepted(
        val products: List<ProductStockInProduct>,
        val gates: List<ProductStockInPlanGate>,
        val filteredMessage: String? = null,
    ) : ProductStockInScanResult

    data class Rejected(val message: String) : ProductStockInScanResult
}
