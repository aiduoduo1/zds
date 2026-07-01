package com.jj.zds.feature.repair.ui

import com.jj.zds.data.dto.repair.RepairAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairSubmitSuccessRulesTest {

    @Test
    fun onlyFormalSubmitPromptsForNextStep() {
        assertFalse(RepairSubmitSuccessRules.shouldPrompt(RepairAction.DRAFT))
        assertTrue(RepairSubmitSuccessRules.shouldPrompt(RepairAction.SUBMIT))
    }

    @Test
    fun handoffKeepsOriginalSnOrProQrCode() {
        assertEquals(
            "PRO||SN260625015||BOX-260625-001",
            RepairSubmitSuccessRules.handoffQrCode(
                "  PRO||SN260625015||BOX-260625-001  ",
            ),
        )
    }
}
