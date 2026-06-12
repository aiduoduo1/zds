package com.jj.zds.data.dto.debugplan

data class DebugPlanListRequest(
    val categoryName: String = "",
    val code: String = "",
    val status: Int? = null,
)
