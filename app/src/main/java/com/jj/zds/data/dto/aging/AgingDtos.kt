package com.jj.zds.data.dto.aging

data class AgingScanValidRequest(
    val qrCode: String,
)

data class AgingStartRequest(
    val agingProList: List<AgingProductDto>,
)

data class AgingEndRequest(
    val id: String,
    val result: String,
    val ngPhenomena: String,
)

data class AgingEndDetailDto(
    val id: String? = null,
    val agingId: String? = null,
    val proId: String? = null,
    val agingStartTime: String? = null,
    val agingStartBy: String? = null,
    val agingEndTime: String? = null,
    val agingEndBy: String? = null,
    val agingResult: String? = null,
    val ngPhenomena: String? = null,
    val reagingResult: String? = null,
    val repairResult: String? = null,
    val code: String? = null,
    val planId: String? = null,
    val planOrderNo: String? = null,
    val partCode: String? = null,
    val partName: String? = null,
    val agingOkQty: Int = 0,
    val agingQty: Int = 0,
)

data class AgingProductDto(
    val id: String? = null,
    val planId: String? = null,
    val proMatId: String? = null,
    val tecDtlId: String? = null,
    val isFqcOk: Boolean? = null,
    val isFqc: Boolean? = null,
    val createBy: String? = null,
    val createTime: String? = null,
    val updateBy: String? = null,
    val updateTime: String? = null,
    val materialCode: String? = null,
    val materialStatus: Int? = null,
    val planOrderCode: String? = null,
    val partId: String? = null,
    val partCode: String? = null,
    val partName: String? = null,
    val circulationCode: String? = null,
    val agingId: String? = null,
    val agingStartTime: String? = null,
    val agingEndTime: String? = null,
    val agingResult: Any? = null,
    val repairResult: Any? = null,
    val sort: Int? = null,
    val createByName: String? = null,
)
