package com.jj.zds.feature.debugplan.data

import com.jj.zds.data.dto.debugplan.DebugPlanBatchDto
import com.jj.zds.data.dto.debugplan.DebugPlanDto
import com.jj.zds.data.dto.debugplan.DebugPlanStatus

object DebugPlanMockData {

    fun createPlans(): List<DebugPlanDto> {
        return listOf(
            DebugPlanDto(
                id = "DBG-20260525-001",
                code = "DBG-20260525-001",
                categoryName = "MAT-6208",
                productName = "伺服驱动模块 SDM-24V",
                supplierName = "华东电子",
                status = 2,
                planCheckQty = 1000,
                sampleQty = 3,
                rejectionQty = 1,
                isForcePhoto = true,
                batches = listOf(
                    DebugPlanBatchDto(
                        no = "B20260525-A01",
                        status = DebugPlanStatus.DOING,
                        boxes = listOf("BX-A01-001"),
                    ),
                ),
            ),
            DebugPlanDto(
                id = "DBG-20260525-002",
                code = "DBG-20260525-002",
                categoryName = "MAT-7718",
                productName = "视觉检测控制板 VCB-8",
                supplierName = "北辰光电",
                status = 1,
                planCheckQty = 600,
                sampleQty = 2,
            ),
            DebugPlanDto(
                id = "DBG-20260524-006",
                code = "DBG-20260524-006",
                categoryName = "MAT-9026",
                productName = "通信采集网关 CAG-4G",
                supplierName = "南部电子",
                status = 3,
                planCheckQty = 200,
                sampleQty = 2,
                isForcePhoto = true,
                batches = listOf(
                    DebugPlanBatchDto(
                        no = "B20260524-C01",
                        status = DebugPlanStatus.DONE,
                        result = "pass",
                    ),
                ),
            ),
        )
    }
}
