package com.jj.zds.data.dto.recheck

import com.jj.zds.data.dto.debugplan.DebugCheckGroupDto

data class RecheckScanValidRequest(
    val qrCode: String,
)

data class RecheckSubmitRequest(
    val qrCode: String,
    val ngPhenomena: String? = null,
    val checkGroupDto: DebugCheckGroupDto,
)
