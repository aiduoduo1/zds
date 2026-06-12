package com.jj.zds.data.dto.debugplan

data class DebugPlanBatchDto(
    val no: String,
    val status: DebugPlanStatus,
    val result: String? = null,
    val boxes: List<String> = emptyList(),
)
