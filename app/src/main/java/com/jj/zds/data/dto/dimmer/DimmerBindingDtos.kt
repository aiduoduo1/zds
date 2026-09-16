package com.jj.zds.data.dto.dimmer

data class DimmerBindingRequest(val stationQr: String, val proQr: String? = null, val airportName: String? = null, val projectName: String? = null, val operatorName: String? = null)
data class DimmerBindingContextDto(val stationCode: String? = null, val stationName: String? = null, val action: String? = null, val binding: DimmerActiveBindingDto? = null, val pro: DimmerProDto? = null)
data class DimmerActiveBindingDto(val proCode: String? = null, val airportName: String? = null, val projectName: String? = null, val planNo: String? = null, val bindAt: String? = null)
data class DimmerProDto(val proCode: String? = null, val planNo: String? = null, val partCode: String? = null, val partName: String? = null)
