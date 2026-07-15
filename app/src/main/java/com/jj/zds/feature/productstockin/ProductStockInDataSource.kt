package com.jj.zds.feature.productstockin

interface ProductStockInDataSource {
    fun scan(code: String): ProductStockInScanResult

    fun confirm(
        products: List<ProductStockInProduct>,
        gates: List<ProductStockInPlanGate>,
    ): ProductStockInConfirmResult
}

class MockProductStockInDataSource : ProductStockInDataSource {

    override fun scan(code: String): ProductStockInScanResult {
        val normalized = code.trim().uppercase()
        val error = when (normalized) {
            "PRO-PRODUCTION-INCOMPLETE" -> "该 PRO 生产尚未完成，不满足入库条件"
            "PRO-AGING-RUNNING" -> "该 PRO 正在老化中，暂不可入库"
            "PRO-AGING-NG" -> "该 PRO 老化结果为 NG，禁止入库"
            "PRO-IN-STOCK" -> "该 PRO 已完成成品入库，不能重复操作"
            "FC-EMPTY" -> "该流转单没有可入库的 PRO"
            else -> null
        }
        if (error != null) return ProductStockInScanResult.Rejected(error)
        if (!normalized.startsWith("PRO") && !normalized.startsWith("FC")) {
            return ProductStockInScanResult.Rejected("无法识别：该条码既不是流转单，也不是 PRO码")
        }
        if (normalized == "FC-PARTIAL") {
            return ProductStockInScanResult.Accepted(
                products = listOf(
                    product("PRO-PART-1001", "驱动器 ZDS-SV-6208", "DRV-6208", "PP20260703002", normalized),
                    product("PRO-PART-1002", "触摸屏 ZDS-SCR-9920", "SCR-9920", "PP20260703003", normalized, true),
                ),
                gates = defaultGates,
                filteredMessage = "流转单共 5 台：已加入 2 台，过滤 3 台（生产未完成、老化中、老化 NG）",
            )
        }
        if (normalized.startsWith("FC")) {
            val suffix = normalized.filter(Char::isLetterOrDigit).takeLast(6)
            return ProductStockInScanResult.Accepted(
                products = listOf(
                    product("PRO-$suffix-001", "控制器 ZDS-CTRL-3000", "CTRL-3000", "PP20260703001", normalized),
                    product("PRO-$suffix-002", "控制器 ZDS-CTRL-3000", "CTRL-3000", "PP20260703001", normalized),
                    product("PRO-$suffix-003", "触摸屏 ZDS-SCR-9920", "SCR-9920", "PP20260703003", normalized, true),
                ),
                gates = defaultGates,
            )
        }
        val planCode = if (normalized.contains("BLOCK")) "PP20260703002" else "PP20260703001"
        return ProductStockInScanResult.Accepted(
            products = listOf(product(normalized, "控制器 ZDS-CTRL-3000", "CTRL-3000", planCode, null)),
            gates = defaultGates,
        )
    }

    override fun confirm(
        products: List<ProductStockInProduct>,
        gates: List<ProductStockInPlanGate>,
    ): ProductStockInConfirmResult = ProductStockInRules.confirm(products, gates)

    private fun product(
        proCode: String,
        productName: String,
        partCode: String,
        planCode: String,
        flowCode: String?,
        agingFree: Boolean = false,
    ) = ProductStockInProduct(
        proCode = proCode,
        productName = productName,
        partCode = partCode,
        planCode = planCode,
        source = flowCode?.let { "流转单 $it" } ?: "单个 PRO",
        gateLabel = if (agingFree) "免老化" else "老化 OK",
    )

    private val defaultGates = listOf(
        ProductStockInPlanGate("PP20260703001", agingOkQty = 8, targetQty = 8),
        ProductStockInPlanGate("PP20260703002", agingOkQty = 3, targetQty = 5),
        ProductStockInPlanGate("PP20260703003", agingOkQty = 0, targetQty = 0, agingFree = true),
    )
}
