package com.jj.zds.feature.repair.ui

import com.jj.zds.data.dto.debugplan.DebugDictItemDto

object RepairResultDisplay {

    fun labelFor(value: String, items: List<DebugDictItemDto>): String {
        val normalizedValue = value.trim()
        if (normalizedValue.isBlank()) return ""
        val matched = items.firstOrNull { it.value?.trim() == normalizedValue }
        return matched?.label?.trim()?.takeIf { it.isNotBlank() }
            ?: matched?.value?.trim()?.takeIf { it.isNotBlank() }
            ?: normalizedValue
    }
}
