package com.jj.zds.feature.repair.data

import com.jj.zds.data.dto.repair.RepairSnScanValidDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairUiStateTest {

    @Test
    fun pendingAndRepairingStatesAreEditable() {
        assertTrue(RepairUiState.from(RepairSnScanValidDto(status = 1, statusName = "待维修")).editable)
        assertTrue(RepairUiState.from(RepairSnScanValidDto(status = 2, statusName = "维修中")).editable)
    }

    @Test
    fun recheckAndClosedStatesAreReadonly() {
        assertFalse(RepairUiState.from(RepairSnScanValidDto(status = 3, statusName = "待复调")).editable)
        assertFalse(RepairUiState.from(RepairSnScanValidDto(status = 4, statusName = "已关闭")).editable)
    }

    @Test
    fun submitIdUsesScannedReportId() {
        val state = RepairUiState.from(RepairSnScanValidDto(id = "255213277351698432"))

        assertEquals(255213277351698432L, state.submitId)
    }
}
