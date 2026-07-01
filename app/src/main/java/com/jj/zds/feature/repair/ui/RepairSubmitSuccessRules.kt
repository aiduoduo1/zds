package com.jj.zds.feature.repair.ui

import com.jj.zds.data.dto.repair.RepairAction

object RepairSubmitSuccessRules {

    fun shouldPrompt(action: RepairAction?): Boolean = action == RepairAction.SUBMIT

    fun handoffQrCode(scanInput: String): String = scanInput.trim()
}
