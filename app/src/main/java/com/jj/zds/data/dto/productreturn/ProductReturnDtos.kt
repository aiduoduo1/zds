package com.jj.zds.data.dto.productreturn

data class AirportDto(
    val id: String? = null,
    val code: String? = null,
    val name: String? = null,
)

data class ProductReturnCreateRequest(val airId: String, val projectName: String)

data class ProductReturnTaskDto(
    val id: String? = null,
    val code: String? = null,
    val airId: String? = null,
    val projectName: String? = null,
    val status: Int? = null,
    val statusLabel: String? = null,
    val createTime: String? = null,
    val airportName: String? = null,
    val createByName: String? = null,
    val returnCount: Int? = null,
)

data class ProductReturnScanRequest(val taskId: String, val qrCode: String)

data class ProductReturnScanResultDto(
    val matId: String? = null,
    val materialCode: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val productName: String? = null,
    val outTaskCode: String? = null,
    val fromAirPortName: String? = null,
)

data class ProductReturnSaveRequest(val taskId: String, val proIdList: List<String>)
data class ProductReturnFinishRequest(val id: String)
