package com.jj.zds.data.dto.productionplan

data class ProductionCirculationProgressRequest(
    val planId: String,
)

data class ProductionCirculationInfoRequest(
    val id: String? = null,
    val qrCode: String? = null,
)

data class ProductionCirculationQueryRequest(
    val circulationId: String? = null,
    val circulationQrCode: String? = null,
)

data class ProductionCirculationPrintRequest(
    val planId: String,
    val num: String,
    val rePrint: Boolean,
    val id: String,
    val type: Int,
)

data class ProductionCirculationMaterialDto(
    val id: String? = null,
    val circulationId: String? = null,
    val materialId: String? = null,
    val materialCode: String? = null,
    val okType: Int? = null,
    val createTime: String? = null,
)

data class ProductionCirculationPrintLogDto(
    val id: String? = null,
    val circulationId: String? = null,
    val printTime: String? = null,
    val createByName: String? = null,
)

data class ProductionCirculationProgressDto(
    val orderCode: String? = null,
    val partName: String? = null,
    val specification: String? = null,
    val unitQuantity: Int = 0,
    val toPrintQuantity: Int = 0,
    val circulationList: List<ProductionCirculationItemDto>? = null,
)

data class ProductionCirculationItemDto(
    val id: String? = null,
    val circulationCode: String? = null,
    val valid: Boolean = true,
    val num: Int = 0,
    val sort: Int = 0,
    val type: Int = 0,
    val printTime: String? = null,
    val printCount: Int = 0,
)

data class ProductionCirculationInfoDto(
    val id: String? = null,
    val circulationCode: String? = null,
    val valid: Boolean? = null,
    val faceNum: Int = 0,
    val num: Int = 0,
    val generateTime: String? = null,
    val printTime: String? = null,
    val printCount: Int = 0,
    val remark: String? = null,
    val orderCode: String? = null,
    val partCode: String? = null,
    val partName: String? = null,
    val specification: String? = null,
    val tecVer: String? = null,
    val lineName: String? = null,
    val createByName: String? = null,
)
