package com.jj.zds.data.dto.productionplan

import java.io.Serializable
import kotlin.jvm.JvmName

data class ProductionPlanListRequest(
    val keyword: String = "",
    val lineId: String? = null,
)

data class PlanRequest(
    val planStatusList: ArrayList<Int>,
    val partId: String? = null,
    val lineId: String? = null,
)

data class ProductionPlanExecuteRequest(
    val planId: String,
)

data class ProductionFinishRequest(
    val planId: String,
    val forceFinish: Boolean,
)

data class ProductionSnBindInfoRequest(
    val planId: String,
    val sn: String,
)

data class ProductionKeyPartBindRequest(
    val planId: String,
    val proSn: String,
    val matSn: String,
    val changeMatSn: String? = null,
    val operateType: Int = 1,
)

data class ProductionSwitchBeginRequest(
    val planId: String,
)

data class ProductionSwitchDetailRequest(
    val planId: String,
)

data class ProductionSoftCheckRequest(
    val lineSwiId: String,
)

data class ProductionMaterialCheckRequest(
    val lineSwiId: String,
)

data class ProductionPersonCheckRequest(
    val lineSwiId: String,
    val tecDtlId: String,
    val staId: String? = null,
    val skiId: String,
    val users: List<ProductionPersonCheckedRequest>,
)

data class ProductionPersonCheckedRequest(
    val id: String? = null,
    val userId: String,
    val activeAt: String? = null,
)

data class ProductionEquipmentCheckRequest(
    val lineSwiId: String,
    val tecDtlId: String,
    val staId: String? = null,
    val equId: String,
    val equCatId: String,
    val equips: List<ProductionEquipmentCheckedRequest>,
)

data class ProductionEquipmentCheckedRequest(
    val id: String? = null,
    val equId: String,
    val activeAt: String? = null,
)

data class ProductionEndSwitchRequest(
    val planId: String,
)

data class ProductionSwitchDetailDto(
    val id: String? = null,
    val planId: String? = null,
    val orderCode: String? = null,
    val partCode: String? = null,
    val equList: List<ProductionSwitchEquipmentDto> = emptyList(),
    val userList: List<ProductionSwitchUserDto> = emptyList(),
    val catList: List<ProductionSwitchCategoryDto> = emptyList(),
    val softList: List<ProductionSwitchSoftDto> = emptyList(),
    val details: Any? = null,
    val stations: Any? = null,
    val force: Boolean? = null,
    val createByName: String? = null,
)

data class ProductionSwitchEquipmentDto(
    val id: String? = null,
    val lineSwiId: String? = null,
    val tecDtlId: String? = null,
    val staId: String? = null,
    val equId: String? = null,
    val equCatId: String? = null,
    val status: Int = 0,
    val staCode: String? = null,
    val staName: String? = null,
    val stationTypeLabel: String? = null,
    val stationCategoryLabel: String? = null,
    val equCatCode: String? = null,
    val equCatName: String? = null,
    val equCode: String? = null,
    val equName: String? = null,
    val equipNum: Int? = null,
    val equips: List<ProductionSwitchCheckedEquipmentDto> = emptyList(),
) : Serializable

data class ProductionSwitchCheckedEquipmentDto(
    val id: String? = null,
    val equId: String? = null,
    val equCode: String? = null,
    val equName: String? = null,
    val activeAt: String? = null,
) : Serializable

data class ProductionSwitchUserDto(
    val id: String? = null,
    val lineSwiId: String? = null,
    val tecDtlId: String? = null,
    val staId: String? = null,
    val userId: String? = null,
    val status: Int = 0,
    val skiId: String? = null,
    val staCode: String? = null,
    val staName: String? = null,
    val stationTypeLabel: String? = null,
    val stationCategoryLabel: String? = null,
    val userName: String? = null,
    val skiName: String? = null,
    val users: List<ProductionSwitchCheckedUserDto> = emptyList(),
) : Serializable

data class ProductionSwitchCheckedUserDto(
    val id: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val account: String? = null,
    val activeAt: String? = null,
) : Serializable

fun ProductionSwitchUserDto.toPersonCheckRequest(
    checkedUsers: List<ProductionPersonCheckedRequest> = users.mapNotNull { user ->
        user.userId?.takeIf { it.isNotBlank() }?.let { userId ->
            ProductionPersonCheckedRequest(
                id = user.id,
                userId = userId,
                activeAt = user.activeAt,
            )
        }
    },
): ProductionPersonCheckRequest {
    return ProductionPersonCheckRequest(
        lineSwiId = lineSwiId.orEmpty(),
        tecDtlId = tecDtlId.orEmpty(),
        staId = staId,
        skiId = skiId.orEmpty(),
        users = checkedUsers,
    )
}

fun ProductionSwitchEquipmentDto.toEquipmentCheckRequest(
    checkedEquips: List<ProductionEquipmentCheckedRequest> = equips.mapNotNull { equip ->
        equip.equId?.takeIf { it.isNotBlank() }?.let { equId ->
            ProductionEquipmentCheckedRequest(
                id = equip.id,
                equId = equId,
                activeAt = equip.activeAt,
            )
        }
    },
): ProductionEquipmentCheckRequest {
    return ProductionEquipmentCheckRequest(
        lineSwiId = lineSwiId.orEmpty(),
        tecDtlId = tecDtlId.orEmpty(),
        staId = staId,
        equId = equId.orEmpty(),
        equCatId = equCatId.orEmpty(),
        equips = checkedEquips,
    )
}

data class ProductionSwitchCategoryDto(
    val id: String? = null,
    val lineSwiId: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val num: String? = null,
    val status: Int = 0,
) : Serializable

data class ProductionSwitchSoftDto(
    val id: String? = null,
    val lineSwiId: String? = null,
    val softGrp: String? = null,
    val softCode: String? = null,
    val softGrpLabel: String? = null,
    val softVerId: String? = null,
    val status: Int = 0,
) : Serializable

data class ProductionSwitchCheckProgress(
    val done: Int,
    val total: Int,
) {
    val state: ProductionSwitchCheckState
        get() = when {
            total <= 0 || done <= 0 -> ProductionSwitchCheckState.WAITING
            done >= total -> ProductionSwitchCheckState.DONE
            else -> ProductionSwitchCheckState.DOING
        }
}

enum class ProductionSwitchCheckState {
    WAITING,
    DOING,
    DONE,
}

fun List<ProductionSwitchEquipmentDto>.toEquipmentCheckProgress(): ProductionSwitchCheckProgress {
    val total = sumOf { it.requiredEquipmentCount() }
    val done = sumOf { item ->
        item.equips.size.coerceAtMost(item.requiredEquipmentCount())
    }
    return ProductionSwitchCheckProgress(done = done, total = total)
}

fun List<ProductionSwitchUserDto>.toPersonCheckProgress(): ProductionSwitchCheckProgress {
    return ProductionSwitchCheckProgress(
        done = count { it.users.isNotEmpty() },
        total = size,
    )
}

@JvmName("materialStatusCheckProgress")
fun List<ProductionSwitchCategoryDto>.toStatusCheckProgress(): ProductionSwitchCheckProgress {
    return ProductionSwitchCheckProgress(
        done = count { it.status == PRODUCTION_SWITCH_STATUS_DONE },
        total = size,
    )
}

@JvmName("softStatusCheckProgress")
fun List<ProductionSwitchSoftDto>.toStatusCheckProgress(): ProductionSwitchCheckProgress {
    return ProductionSwitchCheckProgress(
        done = count { it.status == PRODUCTION_SWITCH_STATUS_DONE },
        total = size,
    )
}

fun ProductionSwitchEquipmentDto.requiredEquipmentCount(): Int {
    return equipNum?.takeIf { it > 0 } ?: 1
}

fun List<ProductionSwitchCategoryDto>.isMaterialCheckComplete(): Boolean {
    return isNotEmpty() && all { it.status == PRODUCTION_SWITCH_STATUS_DONE }
}

fun List<ProductionSwitchCategoryDto>.firstLineSwiId(): String {
    return firstOrNull { !it.lineSwiId.isNullOrBlank() }?.lineSwiId.orEmpty()
}

fun List<ProductionSwitchSoftDto>.isSoftCheckComplete(): Boolean {
    return isNotEmpty() && all { it.status == PRODUCTION_SWITCH_STATUS_DONE }
}

fun ProductionSwitchDetailDto.isAllCheckItemsDone(): Boolean {
    val progresses = buildList {
        if (equList.isNotEmpty()) add(equList.toEquipmentCheckProgress())
        if (userList.isNotEmpty()) add(userList.toPersonCheckProgress())
        if (catList.isNotEmpty()) add(catList.toStatusCheckProgress())
        if (softList.isNotEmpty()) add(softList.toStatusCheckProgress())
    }
    return progresses.isNotEmpty() && progresses.all { it.state == ProductionSwitchCheckState.DONE }
}

private const val PRODUCTION_SWITCH_STATUS_DONE = 1

data class ProductionPlanDto(
    val id: String? = null,
    val orderCode: String? = null,
    val lineId: String? = null,
    val lineName: String? = null,
    val partId: String? = null,
    val partCode: String? = null,
    val partName: String? = null,
    val lot: String? = null,
    val quantity: Int = 0,
    val unit: String? = null,
    val unitStr: String? = null,
    val planDate: String? = null,
    val planStartTime: String? = null,
    val actualStartTime: String? = null,
    val techId: String? = null,
    val planStatus: Int = 0,
    val planStatusName: String? = null,
    val sort: Int = 0,
)

data class ProductionBindProgressDto(
    val orderCode: String? = null,
    val partId: String? = null,
    val partCode: String? = null,
    val finishedCount: Int = 0,
    val totalCount: Int = 0,
)

data class ProductionSnBindInfoDto(
    val planId: String? = null,
    val orderCode: String? = null,
    val sn: String? = null,
    val partId: String? = null,
    val partCode: String? = null,
    val partName: String? = null,
    val currentProcess: String? = null,
    val currentProcessLabel: String? = null,
    val finishedCount: String? = null,
    val totalCount: String? = null,
    val catList: List<ProductionSnBindCategoryDto>? = null,
)

data class ProductionKeyPartBindResultDto(
    val boundMatSns: List<String>? = emptyList(),
)

data class ProductionSnBindCategoryDto(
    val categoryId: String? = null,
    val categoryName: String? = null,
    val productName: String? = null,
    val quantity: Int = 0,
    val matList: List<ProductionSnBindMaterialDto>? = null,
)

data class ProductionSnBindMaterialDto(
    val materialId: String? = null,
    val materialCode: String? = null,
)

data class ProductionStationProgressDto(
    val operationType: Int = 0,
    val finishedCount: Int = 0,
    val totalCount: Int = 0,
)

data class ProductionStationProgressDetailDto(
    val planId: String? = null,
    val orderCode: String? = null,
    val partCode: String? = null,
    val partName: String? = null,
    val staList: List<ProductionStationProgressDto> = emptyList(),
)

data class LineDto(
    val lineId: String? = null,
    val lineName: String = "",
    val toSwitch: Int = 0,
    val inSwitch: Int = 0,
    val stop: Int = 0,
    val cancel: Int = 0,
    val toProduce: Int = 0,
    val inProduce: Int = 0,
    val finish: Int = 0,
)

object ProductionPlanStatus {
    const val WAIT_SWITCH = 1
    const val SWITCHING = 2
    const val WAIT_PRODUCT = 3
    const val PRODUCT_ING = 4
    const val PAUSE = 20

    fun defaultLineCountStatuses(): ArrayList<Int> {
        return arrayListOf(
            WAIT_SWITCH,
            SWITCHING,
            WAIT_PRODUCT,
            PRODUCT_ING,
            PAUSE,
        )
    }
}
