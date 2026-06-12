package com.jj.zds.data.dto.debugplan

data class DebugPlanDto(
    val id: String = "",
    val code: String = "",
    val matCatId: String? = null,
    val supplierCode: String? = null,
    val loc: String? = null,
    val checkGrpId: String? = null,
    val planCheckQty: Int = 0,
    val sampleRatio: Int? = null,
    val sampleQty: Int? = null,
    val rejectionRatio: Int? = null,
    val rejectionQty: Int? = null,
    val isForcePhoto: Boolean = false,
    val isAllowModify: Boolean = false,
    val status: Int = 1,
    val remark: String? = null,
    val createBy: String? = null,
    val createTime: String? = null,
    val updateBy: String? = null,
    val updateTime: String? = null,
    val categoryName: String? = null,
    val productName: String? = null,
    val specification: String? = null,
    val supplierName: String? = null,
    val statusName: String? = null,
    val errorStatusName: String? = null,
    val userName: String? = null,
    val bindBoxCount: Int = 0,
    val bindSnCount: Int = 0,
    val okCount: Int = 0,
    val ngCount: Int = 0,
    val createByName: String? = null,
    val batches: List<DebugPlanBatchDto> = emptyList(),
) {
    val material: String
        get() = productName.orEmpty()

    val materialCode: String
        get() = categoryName.orEmpty()

    val customer: String
        get() = supplierName ?: createByName ?: userName.orEmpty()

    val planStatus: DebugPlanStatus
        get() = DebugPlanStatus.fromCode(status)

    val planQty: Int
        get() = planCheckQty

    val requiredSamples: Int
        get() = sampleQty ?: 0

    val ngLimit: Int
        get() = rejectionQty ?: 0

    val requireSubmitPhoto: Boolean
        get() = isForcePhoto

    val submittedBatchCount: Int
        get() = okCount + ngCount

    val batchText: String
        get() = batches.takeIf { it.isNotEmpty() }
            ?.joinToString(" / ") { it.no }
            ?: buildBindText()

    fun matches(keyword: String): Boolean {
        if (keyword.isBlank()) return true
        val text = buildString {
            append(id).append(' ')
            append(code).append(' ')
            append(material).append(' ')
            append(materialCode).append(' ')
            append(customer).append(' ')
            append(specification.orEmpty()).append(' ')
            append(batches.joinToString(" ") { it.no })
        }
        return text.contains(keyword, ignoreCase = true)
    }

    private fun buildBindText(): String {
        if (bindBoxCount <= 0 && bindSnCount <= 0) {
            return "待创建"
        }
        return "箱 $bindBoxCount / SN $bindSnCount"
    }
}
