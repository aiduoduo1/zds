package com.jj.zds.data.dto.productstockin

data class ProductStockInRequest(
    val qrCode: String,
)

data class ProductStockInResponse(
    val 在库: List<ProductStockInRecordDto> = emptyList(),
    val 已出库: List<ProductStockInRecordDto> = emptyList(),
    val 老化NG: List<ProductStockInRecordDto> = emptyList(),
    val 已入库: List<ProductStockInRecordDto> = emptyList(),
    val 批次未达标: List<ProductStockInRecordDto> = emptyList(),
    val 未老化: List<ProductStockInRecordDto> = emptyList(),
)

/** 成品入库接口返回的本次入库明细。 */
data class ProductStockInRecordDto(
    val displayStatus: String = "",
    val id: String? = null,
    val planId: String? = null,
    val proMatId: String? = null,
    val materialCode: String? = null,
    val materialStatus: Int? = null,
    val planOrderCode: String? = null,
    val partCode: String? = null,
    val partName: String? = null,
    val circulationCode: String? = null,
    val agingOrderNo: String? = null,
    val agingStartTime: String? = null,
    val agingEndTime: String? = null,
    val agingResult: String? = null,
    val warehouseId: String? = null,
    val createTime: String? = null,
)
