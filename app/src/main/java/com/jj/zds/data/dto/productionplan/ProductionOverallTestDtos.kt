package com.jj.zds.data.dto.productionplan

import com.jj.zds.data.dto.debugplan.DebugCheckGroupDto

data class ProductionOverallTestProgressRequest(
    val planId: String,
)

data class ProductionOverallTestSnInfoRequest(
    val planId: String,
    val proSn: String,
)

data class ProductionOverallTestCheckRequest(
    val planId: String,
    val proSn: String,
    val isDraft: Boolean,
    val ngPhenomena: String,
    val checkGroupDto: DebugCheckGroupDto,
)

data class ProductionOverallTestProgressDto(
    val testCount: Int? = null,
    val bindCount: Int? = null,
    val planCount: Int? = null,
    val checkModuleGroupId: String? = null,
) {
    fun subtitle(): String {
        return "测试总进度 ${testCount ?: 0}/${bindCount ?: 0}/${planCount ?: 0}"
    }
}

data class ProductionOverallTestSnInfoDto(
    val proSn: String? = null,
    val currentProcess: Int? = null,
    val currentProcessLabel: String? = null,
    val checkGroupDto: DebugCheckGroupDto? = null,
)

