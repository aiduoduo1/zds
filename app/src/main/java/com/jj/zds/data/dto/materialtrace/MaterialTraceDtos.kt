package com.jj.zds.data.dto.materialtrace

data class MaterialTraceQueryRequest(
    val qrCode: String,
)

data class MaterialTraceInfoDto(
    val materialType: Int = 0,
    val materialCode: String? = null,
    val categoryName: String? = null,
    val productName: String? = null,
    val specification: String? = null,
    val seriesName: String? = null,
    val orderCode: String? = null,
    val lot: String? = null,
    val location: String? = null,
    val lineName: String? = null,
    val keyMatCount: Int? = null,
    val circulationCode: String? = null,
    val bindProCode: String? = null,
    val bindProQrCode: String? = null,
    val materialStatusName: String? = null,
)

data class MaterialTraceBindDto(
    val isBind: Boolean = false,
    val materialCode: String? = null,
    val materialQrCode: String? = null,
    val categoryName: String? = null,
    val productName: String? = null,
)

data class MaterialTraceNodeDto(
    val time: String? = null,
    val title: String? = null,
    val status: String? = null,
    val account: String? = null,
    val operator: String? = null,
    val orderCode: String? = null,
    val remark: String? = null,
)

data class MaterialTraceDetailDto(
    val info: MaterialTraceInfoDto,
    val bindings: List<MaterialTraceBindDto>,
    val nodes: List<MaterialTraceNodeDto>,
)
