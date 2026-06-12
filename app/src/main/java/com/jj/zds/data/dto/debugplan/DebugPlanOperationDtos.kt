package com.jj.zds.data.dto.debugplan

data class DebugBoxScanValidRequest(
    val iqcTaskId: String,
    val boxQrCode: String,
)

data class DebugBoxScanValidDto(
    val box: String? = null,
    val categoryName: String? = null,
    val orderNo: String? = null,
    val lot: String? = null,
    val jobNum: String? = null,
    val qty: Int = 0,
    val date: String? = null,
    val snRange: String? = null,
    val startSn: String? = null,
    val endSn: String? = null,
    val remark: String? = null,
    val snList: List<String> = emptyList(),
    val ver: String? = null,
    val type: String? = null,
    val name: String? = null,
    val snStart: String? = null,
    val snEnd: String? = null,
)

data class DebugBoxBindRequest(
    val iqcTaskId: String,
    val boxQrCodeList: List<String>,
)

data class DebugBoxQrCodeRequest(
    val qrCode: String,
)

data class DebugBoxQrCodeDto(
    val ver: String? = null,
    val type: String? = null,
    val box: String? = null,
    val pn: String? = null,
    val name: String? = null,
    val lot: String? = null,
    val qty: Int = 0,
    val date: String? = null,
    val snStart: String? = null,
    val snEnd: String? = null,
)

data class DebugSnScanValidRequest(
    val taskItemId: String,
    val qrCode: String,
)

data class DebugSnScanValidDto(
    val id: String? = null,
    val snResult: SingleDebugSnResultDto? = null,
    val matId: String? = null,
    val boxLabelId: String? = null,
    val checkModuleId: String? = null,
    val checkResult: String? = null,
    val checkBy: String? = null,
    val checkTime: String? = null,
    val sn: String? = null,
    val boxLabel: String? = null,
    val iqcTaskId: String? = null,
    val qrCode: String? = null,
    val checkGroupDto: DebugCheckGroupDto? = null,
    val checkGrp: DebugCheckGroupDto? = null,
)

data class SingleDebugSnResultDto(
    val sn: String? = null,
    val categoryName: String? = null,
    val orderNo: String? = null,
    val lot: String? = null,
    val jobNum: String? = null,
    val qty: Int = 0,
    val date: String? = null,
    val remark: String? = null,
)

data class DebugCheckGroupDto(
    val id: String? = null,
    val name: String? = null,
    val version: Int = 0,
    val showVersion: String? = null,
    val status: Int = 0,
    val action: String? = null,
    val checkList: List<DebugCheckFirstLevelDto> = emptyList(),
    val matCatList: List<DebugMatCatDto> = emptyList(),
)

data class DebugCheckFirstLevelDto(
    val name: String? = null,
    val sort: Int = 0,
    val children: List<DebugCheckSecondLevelDto> = emptyList(),
)

data class DebugCheckSecondLevelDto(
    val id: String? = null,
    val name: String? = null,
    val sort: Int = 0,
    val children: List<DebugCheckItemDto> = emptyList(),
)

data class DebugCheckItemDto(
    val id: String? = null,
    val itemId: String? = null,
    val name: String? = null,
    val collectType: String? = null,
    val isSkipable: Boolean = false,
    val standardOption: String? = null,
    val minValue: Int? = null,
    val maxValue: Int? = null,
    val description: String? = null,
    val sort: Int = 0,
    val result: Any? = null,
    val createBy: String? = null,
    val updateBy: String? = null,
    val createByName: String? = null,
)

data class DebugMatCatDto(
    val id: String? = null,
    val categoryName: String? = null,
)

data class DebugSnCheckRequest(
    val taskItemId: String,
    val id: String,
    val ngPhenomena: String = "",
    val checkGroupDto: DebugCheckGroupDto,
)

data class SingleDebugCheckRequest(
    val qrCode: String,
    val ngPhenomena: String? = null,
    val checkGroupDto: DebugCheckGroupDto,
)

data class DebugIdRequest(
    val id: String,
)

data class DebugTaskItemInfoDto(
    val id: String? = null,
    val taskId: String? = null,
    val batchNo: String? = null,
    val sort: Int = 0,
    val status: Int = 0,
    val createTime: String? = null,
    val createBy: String? = null,
    val sumCount: Int = 0,
    val okCount: Int = 0,
    val ngCount: Int = 0,
    val statusName: String? = null,
    val boxLabelList: List<String> = emptyList(),
    val updateBy: String? = null,
    val createByName: String? = null,
)

data class DebugPlanFinishRequest(
    val id: String,
    val forcePass: Boolean = false,
    val fileIdList: List<Long> = emptyList(),
)

data class DebugDictRequest(
    val dictCode: String,
)

data class DebugDictItemDto(
    val id: String? = null,
    val value: String? = null,
    val label: String? = null,
    val isShow: Boolean = false,
    val sort: Int = 0,
)
