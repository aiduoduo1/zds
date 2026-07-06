package com.jj.zds.feature.repair.ui

import com.jj.zds.data.dto.debugplan.DebugDictItemDto

object RepairSubmitSuccessRules {

    private const val RECHECK_ALLOWED_LABEL = "已修好，允许复调"

    fun isRecheckAllowed(
        selectedValue: String,
        items: List<DebugDictItemDto>,
    ): Boolean {
        val normalizedValue = selectedValue.trim()
        if (normalizedValue.isBlank()) return false
        return items.firstOrNull { it.value?.trim() == normalizedValue }
            ?.label
            ?.trim() == RECHECK_ALLOWED_LABEL
    }

    fun handoffQrCode(scanInput: String): String = scanInput.trim()
}
