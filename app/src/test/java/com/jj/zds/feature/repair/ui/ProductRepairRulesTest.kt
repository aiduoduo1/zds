package com.jj.zds.feature.repair.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductRepairRulesTest {

    @Test
    fun onlyAgingAndProductionPlanUseFinishedProductRepair() {
        assertTrue(ProductRepairRules.isFinishedProductSource("成品老化"))
        assertTrue(ProductRepairRules.isFinishedProductSource("生产计划"))
        assertFalse(ProductRepairRules.isFinishedProductSource("单个调试"))
        assertFalse(ProductRepairRules.isFinishedProductSource("调试计划"))
    }

    @Test
    fun selectingKeyPartClearsMaterialCode() {
        val selection = ProductFaultSelection(materialCode = "R-1001")
            .selectKeyPart("PART-SN-001", "CAT-001")

        assertEquals("PART-SN-001", selection.keyPartSn)
        assertEquals("CAT-001", selection.keyPartCategoryId)
        assertEquals("", selection.materialCode)
    }

    @Test
    fun enteringMaterialCodeClearsKeyPart() {
        val selection = ProductFaultSelection(keyPartSn = "PART-SN-001", keyPartCategoryId = "CAT-001")
            .enterMaterialCode("IC-5100")

        assertEquals("", selection.keyPartSn)
        assertEquals("", selection.keyPartCategoryId)
        assertEquals("IC-5100", selection.materialCode)
    }

    @Test
    fun okProductSourceCompletesWithoutPageHandoff() {
        assertEquals(
            RepairSubmitOutcome.COMPLETE,
            ProductRepairRules.submitOutcome("成品老化", "OK"),
        )
        assertEquals(
            RepairSubmitOutcome.COMPLETE,
            ProductRepairRules.submitOutcome("生产计划", "OK"),
        )
    }

    @Test
    fun okDebugSourceHandoffsToRecheck() {
        assertEquals(
            RepairSubmitOutcome.RECHECK,
            ProductRepairRules.submitOutcome("单个调试", "OK"),
        )
        assertEquals(
            RepairSubmitOutcome.RECHECK,
            ProductRepairRules.submitOutcome("调试计划", "OK"),
        )
    }

    @Test
    fun nonOkResultIsFinalState() {
        assertEquals(
            RepairSubmitOutcome.FINAL_STATE,
            ProductRepairRules.submitOutcome("调试计划", "NG"),
        )
        assertEquals(
            RepairSubmitOutcome.FINAL_STATE,
            ProductRepairRules.submitOutcome("生产计划", "SCRAP"),
        )
    }
}
