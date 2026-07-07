package com.jj.zds.feature.aging

import com.jj.zds.data.dto.aging.AgingProductDto

data class AgingPendingMergeResult(
    val products: List<AgingProductDto>,
    val addedCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
)

object AgingPendingListRules {

    fun merge(
        existing: List<AgingProductDto>,
        incoming: List<AgingProductDto>,
    ): AgingPendingMergeResult {
        val merged = LinkedHashMap<String, AgingProductDto>()
        existing.forEach { product ->
            product.normalizedProCode()?.let { merged.putIfAbsent(it, product) }
        }

        var addedCount = 0
        var duplicateCount = 0
        var invalidCount = 0
        incoming.forEach { product ->
            val proCode = product.normalizedProCode()
            when {
                proCode == null -> invalidCount++
                merged.containsKey(proCode) -> duplicateCount++
                else -> {
                    merged[proCode] = product
                    addedCount++
                }
            }
        }

        return AgingPendingMergeResult(
            products = merged.values.toList(),
            addedCount = addedCount,
            duplicateCount = duplicateCount,
            invalidCount = invalidCount,
        )
    }

    fun summary(products: List<AgingProductDto>): String {
        val partCount = products.mapNotNull { it.partCode.normalizedValue() }.toSet().size
        val planCount = products.mapNotNull {
            it.planId.normalizedValue() ?: it.planOrderCode.normalizedValue()
        }.toSet().size
        return "共 ${products.size} 台 · $partCount 种产品 · $planCount 个生产计划"
    }

    private fun AgingProductDto.normalizedProCode(): String? {
        return materialCode.normalizedValue()?.uppercase()
    }

    private fun String?.normalizedValue(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }
}
