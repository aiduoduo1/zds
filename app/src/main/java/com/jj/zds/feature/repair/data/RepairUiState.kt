package com.jj.zds.feature.repair.data

import com.jj.zds.data.dto.repair.RepairSnScanValidDto

data class RepairUiState(
    val report: RepairSnScanValidDto,
    val editable: Boolean,
    val submitId: Long,
) {
    companion object {
        fun from(report: RepairSnScanValidDto): RepairUiState {
            return RepairUiState(
                report = report,
                editable = report.status in EDITABLE_STATUS_SET,
                submitId = report.id?.toLongOrNull() ?: 0L,
            )
        }

        private val EDITABLE_STATUS_SET = setOf(STATUS_PENDING, STATUS_REPAIRING)
        private const val STATUS_PENDING = 1
        private const val STATUS_REPAIRING = 2
    }
}
