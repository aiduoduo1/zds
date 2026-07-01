package com.jj.zds.feature.debugplan.ui

import com.jj.zds.data.dto.debugplan.DebugBoxScanValidDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugPlanBoxMaterialValidatorTest {

    @Test
    fun `accepts box when material matches current debug plan`() {
        assertTrue(
            DebugPlanBoxMaterialValidator.isMaterialMatched(
                boxMaterialCode = "MAT-001",
                planMaterialCode = "MAT-001",
            ),
        )
    }

    @Test
    fun `rejects box when material differs from current debug plan`() {
        assertFalse(
            DebugPlanBoxMaterialValidator.isMaterialMatched(
                boxMaterialCode = "MAT-002",
                planMaterialCode = "MAT-001",
            ),
        )
    }

    @Test
    fun `uses parsed categoryName as box material code`() {
        val box = DebugBoxScanValidDto(
            qrCode = "BOX||BOX-01||Test-002",
            categoryName = "Test-002",
        )

        assertEquals("BOX||BOX-01||Test-002", box.qrCode)
        assertFalse(
            DebugPlanBoxMaterialValidator.isMaterialMatched(
                boxMaterialCode = box.categoryName,
                planMaterialCode = "Test-001",
            ),
        )
    }

    @Test
    fun `detects duplicate parsed box number even when sn range differs`() {
        val existingBoxNos = listOf("BOX-01")
        val newBox = DebugBoxScanValidDto(
            box = "BOX-01",
            startSn = "SN-200",
            endSn = "SN-299",
        )

        assertTrue(
            DebugPlanBoxMaterialValidator.isDuplicateBox(
                existingBoxNos = existingBoxNos,
                rawCode = "raw-code-with-different-sn-range",
                parsedBoxNo = newBox.box,
            ),
        )
    }

    @Test
    fun `builds scan-time mismatch message`() {
        assertEquals(
            "BOX-01装箱单不属于物料（MAT-001）",
            DebugPlanBoxMaterialValidator.mismatchMessage(
                boxNo = "BOX-01",
                planMaterialCode = "MAT-001",
            ),
        )
    }
}
