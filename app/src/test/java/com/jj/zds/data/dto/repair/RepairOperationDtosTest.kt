package com.jj.zds.data.dto.repair

import org.junit.Assert.assertEquals
import org.junit.Test

class RepairOperationDtosTest {

    @Test
    fun repairConstantsMatchBackendContract() {
        assertEquals("ngReason", REPAIR_DICT_NG_REASON)
        assertEquals("maintainResult", REPAIR_DICT_MAINTAIN_RESULT)
        assertEquals("DRAFT", RepairAction.DRAFT.value)
        assertEquals("SUBMIT", RepairAction.SUBMIT.value)
    }

    @Test
    fun repairSubmitRequestKeepsOpenApiFieldValues() {
        val request = RepairSubmitRequest(
            action = RepairAction.SUBMIT.value,
            id = 255213277351698432,
            result = "OK",
            ngReason = "焊接问题",
            repairBy = "张泽冰",
            repairDesc = "维修说明",
            fileList = listOf(RepairFileDto(fileId = "123", name = "照片名称")),
        )

        assertEquals("SUBMIT", request.action)
        assertEquals(255213277351698432, request.id)
        assertEquals("OK", request.result)
        assertEquals("焊接问题", request.ngReason)
        assertEquals("张泽冰", request.repairBy)
        assertEquals("维修说明", request.repairDesc)
        assertEquals("123", request.fileList.first().fileId)
        assertEquals("照片名称", request.fileList.first().name)
    }

    @Test
    fun repairFileNameCanBeOmittedWhenSubmittingUploadedPhotoId() {
        val file = RepairFileDto(fileId = "255")

        assertEquals("255", file.fileId)
        assertEquals(null, file.name)
    }

    @Test
    fun repairScanResponseParsesFileObjectList() {
        val response = RepairSnScanValidDto(
            id = "262316381473792000",
            sn = "SN260625015",
            statusName = "待维修",
            fileList = listOf(
                RepairFileDto(
                    fileId = "262316644884422656",
                    name = "avatar2.png",
                ),
            ),
        )

        assertEquals("262316644884422656", response.fileList.first().fileId)
        assertEquals("avatar2.png", response.fileList.first().name)
    }

    @Test
    fun repairLogRequestAndDtoKeepOpenApiFields() {
        val request = RepairLogListRequest(infoId = 255492630060916736)
        val log = RepairLogDto(
            id = "255492630128025600",
            infoId = "255492630060916736",
            opName = "不良上报",
            opDesc = "调试NG上报不良：调光功能失效 状态：待维修",
            opTime = "2026-06-10 14:05:29",
            opBy = "2",
        )

        assertEquals(255492630060916736, request.infoId)
        assertEquals("不良上报", log.opName)
        assertEquals("调试NG上报不良：调光功能失效 状态：待维修", log.opDesc)
        assertEquals("2026-06-10 14:05:29", log.opTime)
        assertEquals("2", log.opBy)
    }
}
