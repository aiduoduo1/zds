package com.jj.zds.data.dto.circulation

data class CirculationQrCodeRequest(
    val qrCode: String,
)

data class CirculationCombineFromCheckRequest(
    val qrCode: String,
    val mainCirculationId: String,
)

data class CirculationProductCheckRequest(
    val circulationId: String,
    val proQrCode: String,
)

data class CirculationDiscardRequest(
    val id: String,
    val operateReason: Int,
    val remark: String,
)

data class CirculationCombineRequest(
    val mainCirculationId: String,
    val fromCirculationId: String,
    val operateReason: Int,
    val remark: String,
)

data class CirculationProductOperationRequest(
    val circulationId: String,
    val proMatId: String,
    val operateReason: Int,
    val remark: String,
)

data class CirculationReasonDto(
    val valueStr: String? = null,
    val label: String? = null,
)

data class CirculationProductCheckDto(
    val id: String? = null,
    val proMatId: String? = null,
    val proMatCode: String? = null,
    val proMatQty: Int? = null,
)
