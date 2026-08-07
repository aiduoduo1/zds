package com.jj.zds.feature.repair.ui

enum class RepairSubmitOutcome {
    RECHECK,
    COMPLETE,
    FINAL_STATE,
}

data class ProductFaultSelection(
    val keyPartSn: String = "",
    val keyPartCategoryId: String = "",
    val materialCode: String = "",
    val replacementSn: String = "",
    val replacementQrCode: String = "",
) {
    fun selectKeyPart(sn: String, categoryId: String): ProductFaultSelection {
        return copy(
            keyPartSn = sn.trim(),
            keyPartCategoryId = categoryId.trim(),
            materialCode = "",
            replacementSn = "",
            replacementQrCode = "",
        )
    }

    fun enterMaterialCode(code: String): ProductFaultSelection {
        return copy(
            keyPartSn = "",
            keyPartCategoryId = "",
            materialCode = code.trim(),
            replacementSn = "",
            replacementQrCode = "",
        )
    }

    fun selectReplacement(sn: String, qrCode: String = ""): ProductFaultSelection {
        return copy(
            replacementSn = sn.trim(),
            replacementQrCode = qrCode.trim(),
        )
    }
}

object ProductRepairRules {

    private const val SOURCE_PRODUCT_AGING = "成品老化"
    private const val SOURCE_SINGLE_DEBUG = "单个调试"
    private const val SOURCE_DEBUG_PLAN = "调试计划"
    private const val SOURCE_PRODUCTION_PLAN = "生产计划"
    private const val RESULT_OK = "OK"

    fun isFinishedProductSource(source: String?): Boolean {
        return source?.trim() in setOf(SOURCE_PRODUCT_AGING, SOURCE_PRODUCTION_PLAN)
    }

    fun isRecheckSource(source: String?): Boolean {
        return source?.trim() in setOf(SOURCE_SINGLE_DEBUG, SOURCE_DEBUG_PLAN)
    }

    fun submitOutcome(
        source: String?,
        selectedValue: String,
        @Suppress("UNUSED_PARAMETER") items: List<Any> = emptyList(),
    ): RepairSubmitOutcome {
        if (selectedValue.trim() != RESULT_OK) return RepairSubmitOutcome.FINAL_STATE
        return when {
            isRecheckSource(source) -> RepairSubmitOutcome.RECHECK
            isFinishedProductSource(source) -> RepairSubmitOutcome.COMPLETE
            else -> RepairSubmitOutcome.FINAL_STATE
        }
    }

    fun isUnrepairableResult(label: String?, value: String?): Boolean {
        val text = label?.trim()?.takeIf { it.isNotBlank() }
            ?: value?.trim().orEmpty()
        return text.contains("修不好") || text.contains("报废")
    }
}
