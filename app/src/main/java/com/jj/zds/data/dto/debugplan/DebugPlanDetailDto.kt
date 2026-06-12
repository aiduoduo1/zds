package com.jj.zds.data.dto.debugplan

data class DebugPlanDetailDto(
    val code: String? = null,
    val categoryName: String? = null,
    val productName: String? = null,
    val specification: String? = null,
    val sampleQty: Int = 0,
    val checkQty: Int = 0,
    val rejectionQty: Int = 0,
    val ngCount: Int = 0,
    val count: Int = 0,
    val finishCount: Int = 0,
    val list: List<DebugPlanTaskItemDto> = emptyList(),
)

data class DebugPlanTaskItemDto(
    val id: String? = null,
    val taskId: String? = null,
    val batchNo: String? = null,
    val sort: Int = 0,
    val status: Int = 0,
    val createTime: String? = null,
    val createBy: String? = null,
    val updateBy: String? = null,
    val createByName: String? = null,
    val boxLabelList: List<DebugPlanBoxLabelDto> = emptyList(),
)

data class DebugPlanBoxLabelDto(
    val id: String? = null,
    val iqcTaskId: String? = null,
    val taskItemId: String? = null,
    val boxLabel: String? = null,
    val boxQrCode: String? = null,
    val quantity: Int = 0,
    val startSn: String? = null,
    val endSn: String? = null,
    val bindBy: String? = null,
    val bindTime: String? = null,
    val status: Int = 0,
    val updateBy: String? = null,
    val createBy: String? = null,
    val createByName: String? = null,
)
