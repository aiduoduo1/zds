package com.jj.zds.feature.debugplan.ui

internal object DebugPlanBoxMaterialValidator {

    fun isMaterialMatched(
        boxMaterialCode: String?,
        planMaterialCode: String?,
    ): Boolean {
        val boxMaterial = boxMaterialCode.orEmpty().trim()
        val planMaterial = planMaterialCode.orEmpty().trim()
        if (boxMaterial.isBlank() || planMaterial.isBlank()) return true
        return boxMaterial.equals(planMaterial, ignoreCase = true)
    }

    fun mismatchMessage(
        boxNo: String,
        planMaterialCode: String,
    ): String {
        val boxLabel = boxNo.trim().ifBlank { "当前" }
        val planMaterial = planMaterialCode.trim().ifBlank { "-" }
        return "${boxLabel}装箱单不属于物料（$planMaterial）"
    }

    fun isDuplicateBox(
        existingBoxNos: List<String>,
        rawCode: String,
        parsedBoxNo: String?,
    ): Boolean {
        val parsedKey = parsedBoxNo.normalizeBoxKey()
        val rawKey = rawCode.normalizeBoxKey()
        return existingBoxNos
            .map { it.normalizeBoxKey() }
            .any { it.isNotBlank() && (it == parsedKey || it == rawKey) }
    }

    private fun String?.normalizeBoxKey(): String {
        return this.orEmpty().trim().uppercase()
    }
}
