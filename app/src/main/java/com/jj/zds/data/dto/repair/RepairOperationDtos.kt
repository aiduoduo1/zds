package com.jj.zds.data.dto.repair

const val REPAIR_DICT_NG_REASON = "ngReason"
const val REPAIR_DICT_MAINTAIN_RESULT = "maintainResult"

enum class RepairAction(val value: String) {
    DRAFT("DRAFT"),
    SUBMIT("SUBMIT"),
}

data class RepairSnScanValidRequest(
    val qrCode: String,
)

data class RepairSnScanValidDto(
    val id: String? = null,
    val ngSource: String? = null,
    val matId: String? = null,
    val sourceId: String? = null,
    val planNumber: String? = null,
    val batchNumber: String? = null,
    val checkGrpId: String? = null,
    val boxLabel: String? = null,
    val ngPhenomena: String? = null,
    val ngRemark: String? = null,
    val ngReason: String? = null,
    val result: String? = null,
    val status: Int = 0,
    val createTime: String? = null,
    val createBy: String? = null,
    val repairTime: String? = null,
    val repairBy: String? = null,
    val repairDesc: String? = null,
    val sn: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val productName: String? = null,
    val statusName: String? = null,
    val userName: String? = null,
    val fileList: List<RepairFileDto> = emptyList(),
    val updateBy: String? = null,
    val createByName: String? = null,
)

data class RepairSubmitRequest(
    val action: String,
    val id: Long,
    val result: String,
    val ngReason: String,
    val repairBy: String,
    val repairDesc: String,
    val fileList: List<RepairFileDto> = emptyList(),
)

data class RepairFileDto(
    val fileId: String,
    val name: String? = null,
)

data class RepairLogListRequest(
    val infoId: Long,
)

data class RepairLogDto(
    val id: String? = null,
    val infoId: String? = null,
    val opName: String? = null,
    val opDesc: String? = null,
    val opTime: String? = null,
    val opBy: String? = null,
    val createBy: String? = null,
    val updateBy: String? = null,
    val createByName: String? = null,
)
