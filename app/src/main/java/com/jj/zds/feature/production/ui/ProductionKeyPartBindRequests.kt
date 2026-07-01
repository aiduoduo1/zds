package com.jj.zds.feature.production.ui

import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindRequest
import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindResultDto

object ProductionKeyPartBindRequests {

    fun bind(planId: String, proSn: String, matSn: String) = ProductionKeyPartBindRequest(
        planId = planId,
        proSn = proSn,
        matSn = matSn,
        operateType = 1,
    )

    fun unbind(planId: String, proSn: String, matSn: String) = ProductionKeyPartBindRequest(
        planId = planId,
        proSn = proSn,
        matSn = matSn,
        operateType = 2,
    )

    fun replace(
        planId: String,
        proSn: String,
        matSn: String,
        changeMatSn: String,
    ) = ProductionKeyPartBindRequest(
        planId = planId,
        proSn = proSn,
        matSn = matSn,
        changeMatSn = changeMatSn,
        operateType = 3,
    )

    fun replacementCandidates(result: ProductionKeyPartBindResultDto): List<String> {
        return result.boundMatSns.orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
