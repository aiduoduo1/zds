package com.jj.zds.data.repository

import com.jj.common.exception.JiangErrorCode
import com.jj.common.exception.JiangException
import com.jj.common.result.JiangResult
import com.jj.network.JiangNetwork
import com.jj.network.call.JiangApiCaller
import com.jj.storage.JiangStorage
import com.jj.zds.data.api.ZdsApi
import com.jj.zds.data.dto.common.CurUserDto
import com.jj.zds.data.dto.common.UserSkillRequest
import com.jj.zds.data.dto.common.ZdsBaseResult
import com.jj.zds.data.dto.common.toJiangResult
import com.jj.zds.data.dto.debugplan.DebugBoxBindRequest
import com.jj.zds.data.dto.debugplan.DebugBoxQrCodeDto
import com.jj.zds.data.dto.debugplan.DebugBoxQrCodeRequest
import com.jj.zds.data.dto.debugplan.DebugBoxScanValidDto
import com.jj.zds.data.dto.debugplan.DebugBoxScanValidRequest
import com.jj.zds.data.dto.debugplan.DebugCheckGroupDto
import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import com.jj.zds.data.dto.debugplan.DebugDictRequest
import com.jj.zds.data.dto.debugplan.DebugPlanDetailDto
import com.jj.zds.data.dto.debugplan.DebugPlanDetailRequest
import com.jj.zds.data.dto.debugplan.DebugPlanDto
import com.jj.zds.data.dto.debugplan.DebugIdRequest
import com.jj.zds.data.dto.debugplan.DebugPlanFinishRequest
import com.jj.zds.data.dto.debugplan.DebugPlanListRequest
import com.jj.zds.data.dto.debugplan.DebugPlanStatisticsDto
import com.jj.zds.data.dto.debugplan.DebugSnCheckRequest
import com.jj.zds.data.dto.debugplan.DebugSnScanValidDto
import com.jj.zds.data.dto.debugplan.DebugSnScanValidRequest
import com.jj.zds.data.dto.debugplan.SingleDebugCheckRequest
import com.jj.zds.data.dto.debugplan.DebugTaskItemInfoDto
import com.jj.zds.data.dto.debugplan.DebugTaskLogRequest
import com.jj.zds.data.dto.login.LoginRequest
import com.jj.zds.data.dto.menu.ZdsMenuDto
import com.jj.zds.data.dto.productionplan.LineDto
import com.jj.zds.data.dto.productionplan.PlanRequest
import com.jj.zds.data.dto.productionplan.ProductionBindProgressDto
import com.jj.zds.data.dto.productionplan.ProductionEndSwitchRequest
import com.jj.zds.data.dto.productionplan.ProductionEquipmentCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindRequest
import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindResultDto
import com.jj.zds.data.dto.productionplan.ProductionPlanExecuteRequest
import com.jj.zds.data.dto.productionplan.ProductionMaterialCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionPersonCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionPlanDto
import com.jj.zds.data.dto.productionplan.ProductionPlanListRequest
import com.jj.zds.data.dto.productionplan.ProductionPlanStatus
import com.jj.zds.data.dto.productionplan.ProductionSoftCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionSnBindInfoDto
import com.jj.zds.data.dto.productionplan.ProductionSnBindInfoRequest
import com.jj.zds.data.dto.productionplan.ProductionStationProgressDto
import com.jj.zds.data.dto.productionplan.ProductionSwitchBeginRequest
import com.jj.zds.data.dto.productionplan.ProductionSwitchDetailDto
import com.jj.zds.data.dto.productionplan.ProductionSwitchDetailRequest
import com.jj.zds.data.dto.qrcode.QrCodeDto
import com.jj.zds.data.dto.recheck.RecheckScanValidRequest
import com.jj.zds.data.dto.recheck.RecheckSubmitRequest
import com.jj.zds.data.dto.repair.REPAIR_DICT_MAINTAIN_RESULT
import com.jj.zds.data.dto.repair.REPAIR_DICT_NG_REASON
import com.jj.zds.data.dto.repair.RepairLogDto
import com.jj.zds.data.dto.repair.RepairLogListRequest
import com.jj.zds.data.dto.repair.RepairSnScanValidDto
import com.jj.zds.data.dto.repair.RepairSnScanValidRequest
import com.jj.zds.data.dto.repair.RepairSubmitRequest
import com.jj.zds.data.dto.update.ZdsVersionCheckResult
import com.jj.zds.data.local.ZdsStorageKeys
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class ZdsRepository(
    private val api: ZdsApi = JiangNetwork.createApi(ZdsApi::class.java),
) {

    /**
     * 登录只负责换取 token；拿到 token 后立即落本地，后续接口会依赖它拼接鉴权信息。
     */
    suspend fun login(request: LoginRequest): JiangResult<String> {
        return when (val result = JiangApiCaller.safeApiCall { api.login(request) }) {
            is JiangResult.Success -> saveTokenResult(
                result.data.toJiangResult(
                    emptyMessage = "Login token is empty.",
                ),
            )
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    /**
     * 登录成功后拉取当前用户信息，并缓存用户基础字段，供页面展示和权限判断使用。
     */
    suspend fun initUser(): JiangResult<CurUserDto> {
        return when (val result = JiangApiCaller.safeApiCall { api.initUser() }) {
            is JiangResult.Success -> saveUserResult(
                result.data.toJiangResult(
                    emptyMessage = "Current user is empty.",
                ),
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    /**
     * 登录页的组合流程：先保存 token，再用 token 初始化当前用户。
     * 调试登录问题时优先看 login() 是否成功，再看 initUser() 是否返回用户信息。
     */
    suspend fun initUserByAccount(
        account: String,
        status: Int = 1,
        distinct: Boolean = true,
    ): JiangResult<CurUserDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.initUserByAccount(
                    UserSkillRequest(
                        account = account,
                        status = status,
                        distinct = distinct,
                    ),
                )
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "User is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun loginAndInitUser(request: LoginRequest): JiangResult<CurUserDto> {
        return when (val loginResult = login(request)) {
            is JiangResult.Success -> initUser()
            is JiangResult.Error -> loginResult
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getMenu(menuType: Int = 2): JiangResult<List<ZdsMenuDto>> {
        return when (val result = JiangApiCaller.safeApiCall { api.getMenu(mapOf("menuType" to menuType)) }) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Menu is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    /**
     * 检查 APP 版本。后端成功但 data 为空表示无更新，不按空数据错误处理。
     */
    suspend fun versionCheck(versionName: String): JiangResult<ZdsVersionCheckResult> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.versionCheck(
                    mapOf(
                        "versionCode" to versionName,
                        "clientType" to CLIENT_TYPE_ANDROID,
                    ),
                )
            }
        ) {
            is JiangResult.Success -> {
                val response = result.data
                if (!response.isSuccess()) {
                    JiangResult.Error(
                        JiangException(
                            code = response.code,
                            message = response.message ?: response.msg ?: "Version check failed.",
                        ),
                    )
                } else {
                    val update = response.data
                    if (update == null) {
                        JiangResult.Success(
                            ZdsVersionCheckResult.NoUpdate(
                                response.message ?: response.msg ?: "Already latest version.",
                            ),
                        )
                    } else {
                        JiangResult.Success(ZdsVersionCheckResult.HasUpdate(update))
                    }
                }
            }

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun decodeQrCode(code: String): JiangResult<QrCodeDto> {
        return when (val result = JiangApiCaller.safeApiCall { api.decodeQrCode(code) }) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "QR code data is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun scanValidRepairSn(qrCode: String): JiangResult<RepairSnScanValidDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.scanValidRepairSn(RepairSnScanValidRequest(qrCode = qrCode))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Repair SN data is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getDebugPlanStatistics(): JiangResult<DebugPlanStatisticsDto> {
        return when (val result = JiangApiCaller.safeApiCall { api.getDebugPlanStatistics() }) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Debug plan statistics is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getDebugPlanList(request: DebugPlanListRequest): JiangResult<List<DebugPlanDto>> {
        return when (val result = JiangApiCaller.safeApiCall { api.getDebugPlanList(request) }) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Debug plan list is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getProductionPlanList(
        keyword: String = "",
        lineId: String? = null,
    ): JiangResult<List<ProductionPlanDto>> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getProductionPlanList(
                    ProductionPlanListRequest(
                        keyword = keyword,
                        lineId = lineId?.trim()?.takeIf { it.isNotBlank() },
                    ),
                )
            }
        ) {
            is JiangResult.Success -> {
                val response = result.data
                if (response.isSuccess() && response.data == null) {
                    JiangResult.Success(emptyList())
                } else {
                    response.toJiangResult(
                        emptyMessage = "Production plan list is empty.",
                    )
                }
            }

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getProductionLineCount(
        planStatusList: ArrayList<Int> = ProductionPlanStatus.defaultLineCountStatuses(),
        partId: String? = null,
        lineId: String? = null,
    ): JiangResult<List<LineDto>> {
        return getProductionLineCount(
            PlanRequest(
                planStatusList = planStatusList,
                partId = partId,
                lineId = lineId,
            ),
        )
    }

    suspend fun getProductionLineCount(request: PlanRequest): JiangResult<List<LineDto>> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getProductionLineCount(request)
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Production line count list is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getProductionStationProgressList(planId: String): JiangResult<List<ProductionStationProgressDto>> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getProductionStationProgressList(ProductionPlanExecuteRequest(planId = planId))
            }
        ) {
            is JiangResult.Success -> {
                val response = result.data
                if (response.isSuccess()) {
                    JiangResult.Success(response.data?.staList.orEmpty())
                } else {
                    JiangResult.Error(
                        JiangException(
                            code = response.code,
                            message = response.message ?: response.msg ?: "Production progress request failed.",
                        ),
                    )
                }
            }
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Success(emptyList())
        }
    }

    suspend fun getProductionBindProgress(planId: String): JiangResult<ProductionBindProgressDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getProductionBindProgress(ProductionPlanExecuteRequest(planId = planId))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Production bind progress is empty.",
            )
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getProductionSnBindInfo(planId: String, sn: String): JiangResult<ProductionSnBindInfoDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getProductionSnBindInfo(
                    ProductionSnBindInfoRequest(
                        planId = planId,
                        sn = sn,
                    ),
                )
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Production SN bind info is empty.",
            )
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun bindProductionKeyPart(
        request: ProductionKeyPartBindRequest,
    ): JiangResult<ProductionKeyPartBindResultDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.bindProductionKeyPart(request)
            }
        ) {
            is JiangResult.Success -> {
                val response = result.data
                if (response.code == CODE_KEY_PART_ALREADY_BOUND && response.data != null) {
                    JiangResult.Success(response.data)
                } else {
                    response.toJiangResult(
                        emptyMessage = "Production key part bind result is empty.",
                    )
                }
            }
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun startProduction(planId: String): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.startProduction(ProductionPlanExecuteRequest(planId = planId))
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun beginProductionSwitch(planId: String): JiangResult<Int> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.beginProductionSwitch(ProductionSwitchBeginRequest(planId = planId))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Production switch id is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getProductionSwitchDetail(planId: String): JiangResult<ProductionSwitchDetailDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getProductionSwitchDetail(ProductionSwitchDetailRequest(planId = planId))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Production switch detail is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun confirmProductionSoftCheck(lineSwiId: String): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.confirmProductionSoftCheck(ProductionSoftCheckRequest(lineSwiId = lineSwiId))
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun confirmProductionMaterialCheck(lineSwiId: String): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.confirmProductionMaterialCheck(ProductionMaterialCheckRequest(lineSwiId = lineSwiId))
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun confirmProductionPersonCheck(
        requests: List<ProductionPersonCheckRequest>,
    ): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.confirmProductionPersonCheck(requests)
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun confirmProductionEquipmentCheck(
        requests: List<ProductionEquipmentCheckRequest>,
    ): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.confirmProductionEquipmentCheck(requests)
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun endProductionSwitch(planId: String): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.endProductionSwitch(ProductionEndSwitchRequest(planId = planId))
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getDebugPlanDetail(taskId: String): JiangResult<DebugPlanDetailDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getDebugPlanDetail(DebugPlanDetailRequest(taskId = taskId))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Debug plan detail is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    /**
     * 校验调试计划的料箱码是否允许绑定到当前任务。
     */
    suspend fun scanValidDebugBox(
        iqcTaskId: String,
        boxQrCode: String,
    ): JiangResult<DebugBoxScanValidDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.scanValidDebugBox(
                    DebugBoxScanValidRequest(
                        iqcTaskId = iqcTaskId,
                        boxQrCode = boxQrCode,
                    ),
                )
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Box scan data is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    /**
     * 创建批次时一次性提交本地待绑定的料箱码列表。
     */
    suspend fun bindDebugBoxes(
        iqcTaskId: String,
        boxQrCodeList: List<String>,
    ): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.bindDebugBoxes(
                    DebugBoxBindRequest(
                        iqcTaskId = iqcTaskId,
                        boxQrCodeList = boxQrCodeList,
                    ),
                )
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }



    /**
     * 扫描 SN 后向后端校验，并取回该 SN 对应的点检表单结构。
     */
    suspend fun scanValidDebugSn(
        taskItemId: String,
        qrCode: String,
    ): JiangResult<DebugSnScanValidDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.scanValidDebugSn(
                    DebugSnScanValidRequest(
                        taskItemId = taskItemId,
                        qrCode = qrCode,
                    ),
                )
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "SN scan data is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    /**
     * 提交单个 SN 的点检结果。checkGroupDto 会携带页面上回填后的每个检查项结果。
     */
    suspend fun submitDebugSnCheck(
        taskItemId: String,
        snRecordId: String,
        ngPhenomena: String,
        checkGroupDto: DebugCheckGroupDto,
        isDraft: Boolean,
    ): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.submitDebugSnCheck(
                    DebugSnCheckRequest(
                        taskItemId = taskItemId,
                        id = snRecordId,
                        ngPhenomena = ngPhenomena,
                        checkGroupDto = checkGroupDto,
                        isDraft = isDraft,
                    ),
                )
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun saveDebugTaskLog(request: DebugTaskLogRequest): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.saveDebugTaskLog(request)
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun scanValidSingleDebugSn(qrCode: String): JiangResult<DebugSnScanValidDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.scanValidSingleDebugSn(DebugBoxQrCodeRequest(qrCode = qrCode))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Single debug scan data is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun submitSingleDebugCheck(
        qrCode: String,
        ngPhenomena: String?,
        checkGroupDto: DebugCheckGroupDto,
        isDraft: Boolean,
    ): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.submitSingleDebugCheck(
                    SingleDebugCheckRequest(
                        qrCode = qrCode,
                        ngPhenomena = ngPhenomena,
                        checkGroupDto = checkGroupDto,
                        isDraft = isDraft,
                    ),
                )
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun scanValidRecheckSn(qrCode: String): JiangResult<DebugSnScanValidDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.scanValidRecheckSn(RecheckScanValidRequest(qrCode = qrCode))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Recheck scan data is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun submitRecheck(
        qrCode: String,
        ngPhenomena: String?,
        checkGroupDto: DebugCheckGroupDto,
    ): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.submitRecheck(
                    RecheckSubmitRequest(
                        qrCode = qrCode,
                        ngPhenomena = ngPhenomena,
                        checkGroupDto = checkGroupDto,
                    ),
                )
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getDebugTaskItemInfo(id: String): JiangResult<DebugTaskItemInfoDto> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getDebugTaskItemInfo(DebugIdRequest(id = id))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Debug task item info is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    /**
     * 批次完工接口，只提交批次 id，页面收到成功后会刷新批次状态。
     */
    suspend fun finishDebugTaskItem(id: String): JiangResult<Unit> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.finishDebugTaskItem(DebugIdRequest(id = id))
            }
        ) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    /**
     * 调试计划完工。后端可能返回未满足完工条件的提示 map，不能简单套用 toUnitResult()。
     */
    suspend fun finishDebugPlan(
        id: String,
        fileIdList: List<Long>,
        forcePass: Boolean = false,
    ): JiangResult<Map<String, String>?> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.finishDebugPlan(
                    DebugPlanFinishRequest(
                        id = id,
                        forcePass = forcePass,
                        fileIdList = fileIdList,
                    ),
                )
            }
        ) {
            is JiangResult.Success -> {
                val responseText = result.data.string()
                val response = parseBaseResponse(responseText)
                if (response.code == ZdsBaseResult.CODE_SUCCESS) {
                    JiangResult.Success<Map<String, String>?>(null)
                } else {
                    JiangResult.Error(
                        JiangException(
                            code = response.code,
                            message = response.message ?: "Debug plan finish failed.",
                        ),
                    )
                }
            }

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun uploadFile(file: File): JiangResult<Long> {
        if (!file.exists() || file.length() <= 0L) {
            return JiangResult.Error(
                JiangException(
                    code = JiangErrorCode.UNKNOWN,
                    message = "Upload file is empty.",
                ),
            )
        }

        return when (
            val result = JiangApiCaller.safeApiCall {
                val requestBody = file.asRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
                api.upload(listOf(part))
            }
        ) {
            is JiangResult.Success -> {
                val responseText = result.data.string().trim()
                val numericFileId = parseUploadFileId(responseText)
                if (numericFileId == null) {
                    JiangResult.Error(
                        JiangException(
                            code = JiangErrorCode.UNKNOWN,
                            message = "Upload returned invalid file id: $responseText",
                        ),
                    )
                } else {
                    JiangResult.Success(numericFileId)
                }
            }

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    private fun parseUploadFileId(responseText: String): Long? {
        if (responseText.isBlank()) return null
        responseText.unquoteJsonString().toLongOrNull()?.let { return it }

        val json = responseText.toJsonValueOrNull()
        return json.findLongValue("data")
            ?: json.findLongValue("id")
            ?: json.findLongValue("fileId")
            ?: json.findLongValue("fileID")
    }

    private fun parseBaseResponse(responseText: String): RawBaseResponse {
        val jsonObject = responseText.toJsonValueOrNull() as? JSONObject
            ?: return RawBaseResponse(
                code = JiangErrorCode.UNKNOWN,
                message = "Invalid response: $responseText",
                data = null,
            )
        return RawBaseResponse(
            code = jsonObject.optInt("code", JiangErrorCode.UNKNOWN),
            message = jsonObject.optStringOrNull("message") ?: jsonObject.optStringOrNull("msg"),
            data = jsonObject.optNullable("data"),
        )
    }

    private fun Any?.toFinishMessageMap(): Map<String, String>? {
        return when (this) {
            null, JSONObject.NULL -> null
            is JSONObject -> keys().asSequence()
                .mapNotNull { key ->
                    val content = optNullable(key).toDisplayText().takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    key to content
                }
                .toMap()
                .takeIf { it.isNotEmpty() }

            is JSONArray -> (0 until length())
                .mapNotNull { index ->
                    optNullable(index).toDisplayText().takeIf { it.isNotBlank() }
                }
                .takeIf { it.isNotEmpty() }
                ?.mapIndexed { index, text -> "提示${index + 1}" to text }
                ?.toMap()

            else -> toDisplayText().takeIf { it.isNotBlank() }?.let { mapOf("提示" to it) }
        }
    }

    private fun Any?.findLongValue(key: String): Long? {
        return when (this) {
            null, JSONObject.NULL -> null
            is Number -> toLong()
            is String -> unquoteJsonString().toLongOrNull()
            is JSONObject -> {
                optNullable(key).findLongValue("")
                    ?: optNullable("data").findLongValue("id")
                    ?: optNullable("id").findLongValue("")
                    ?: optNullable("fileId").findLongValue("")
                    ?: optNullable("fileID").findLongValue("")
            }

            else -> toString().unquoteJsonString().toLongOrNull()
        }
    }

    private fun Any?.toDisplayText(): String {
        return when (this) {
            null, JSONObject.NULL -> ""
            is JSONObject -> {
                listOf("message", "msg", "name", "label", "value", "reason")
                    .firstNotNullOfOrNull { key -> optNullable(key).toDisplayText().takeIf { it.isNotBlank() } }
                    ?: toString()
            }

            is JSONArray -> (0 until length()).joinToString("；") { index ->
                optNullable(index).toDisplayText()
            }

            else -> toString()
        }
    }

    private fun String.toJsonValueOrNull(): Any? {
        return runCatching { JSONTokener(this).nextValue() }.getOrNull()
    }

    private fun String.unquoteJsonString(): String {
        return if (length >= 2 && first() == '"' && last() == '"') {
            substring(1, length - 1)
        } else {
            this
        }.trim()
    }

    private fun JSONObject.optNullable(key: String): Any? {
        return if (has(key)) opt(key) else null
    }

    private fun JSONArray.optNullable(index: Int): Any? {
        return if (index in 0 until length()) opt(index) else null
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return optNullable(key)?.toString()?.takeIf { it.isNotBlank() && it != "null" }
    }

    private data class RawBaseResponse(
        val code: Int,
        val message: String?,
        val data: Any?,
    )

    suspend fun getDebugDictItems(dictCode: String): JiangResult<List<DebugDictItemDto>> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getDebugDictItems(DebugDictRequest(dictCode = dictCode))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Dictionary item list is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getRepairNgReasonItems(): JiangResult<List<DebugDictItemDto>> {
        return getRepairDictItems(REPAIR_DICT_NG_REASON)
    }

    suspend fun getRepairResultItems(): JiangResult<List<DebugDictItemDto>> {
        return getRepairDictItems(REPAIR_DICT_MAINTAIN_RESULT)
    }

    suspend fun submitRepair(request: RepairSubmitRequest): JiangResult<Unit> {
        return when (val result = JiangApiCaller.safeApiCall { api.submitRepair(request) }) {
            is JiangResult.Success -> result.data.toUnitResult()
            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    suspend fun getRepairLogs(infoId: Long): JiangResult<List<RepairLogDto>> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getRepairLogs(RepairLogListRequest(infoId = infoId))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Repair log list is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    private suspend fun getRepairDictItems(dictCode: String): JiangResult<List<DebugDictItemDto>> {
        return when (
            val result = JiangApiCaller.safeApiCall {
                api.getRepairDictItems(DebugDictRequest(dictCode = dictCode))
            }
        ) {
            is JiangResult.Success -> result.data.toJiangResult(
                emptyMessage = "Repair dictionary item list is empty.",
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    /**
     * 下载升级包使用独立 downloader，并把框架进度对象转换成页面只关心的百分比。
     */
    suspend fun downloadUpdateApk(
        fileId: String,
        targetFile: File,
        onProgress: (Int) -> Unit,
    ): JiangResult<File> {
        return try {
            val result = withContext(Dispatchers.IO) {
                JiangNetwork.createApkDownloader().downloadApk(
                    url = buildUpdateApkUrl(fileId),
                    targetFile = targetFile,
                    onProgress = { progress ->
                        onProgress(progress.percent)
                    },
                )
            }
            JiangResult.Success(result.file)
        } catch (throwable: Throwable) {
            JiangResult.Error(
                JiangException(
                    code = JiangErrorCode.UNKNOWN,
                    message = throwable.message ?: "APK download failed.",
                    throwable = throwable,
                ),
            )
        }
    }

    fun hasToken(): Boolean {
        return getToken().isNotBlank()
    }

    /**
     * 清除所有登录相关缓存；退出登录和 token 失效时都走这里。
     */
    fun clearLogin() {
        JiangStorage.kv.remove(ZdsStorageKeys.MES_UP_TOKEN)
        JiangStorage.kv.remove(ZdsStorageKeys.USER_ID)
        JiangStorage.kv.remove(ZdsStorageKeys.USER_NAME)
        JiangStorage.kv.remove(ZdsStorageKeys.USER_SKILLS)
    }

    /**
     * token 为空时即使后端 code 成功也转成错误，避免后续页面进入半登录状态。
     */
    private fun saveTokenResult(result: JiangResult<String>): JiangResult<String> {
        if (result is JiangResult.Success && result.data.isBlank()) {
            return JiangResult.Error(
                JiangException(
                    code = JiangErrorCode.UNKNOWN,
                    message = "Login token is empty.",
                ),
            )
        }
        if (result is JiangResult.Success) {
            JiangStorage.kv.putString(ZdsStorageKeys.MES_UP_TOKEN, result.data)
        }
        return result
    }

    /**
     * 用户信息缓存只保存页面当前需要的字段，避免把完整用户对象和接口结构强绑定。
     */
    private fun saveUserResult(result: JiangResult<CurUserDto>): JiangResult<CurUserDto> {
        if (result is JiangResult.Success) {
            val user = result.data
            JiangStorage.kv.putString(ZdsStorageKeys.USER_ID, user.id.orEmpty())
            JiangStorage.kv.putString(ZdsStorageKeys.USER_NAME, user.userName.orEmpty())
            JiangStorage.kv.putString(
                ZdsStorageKeys.USER_SKILLS,
                user.skills.orEmpty().joinToString(","),
            )
        }
        return result
    }

    private fun getToken(): String {
        return JiangStorage.kv.getString(ZdsStorageKeys.MES_UP_TOKEN, "").orEmpty()
    }

    private fun buildUpdateApkUrl(fileId: String): String {
        return "task/taskFile/download/$fileId?token=${getToken()}"
    }

    /**
     * 适配后端“只返回成功/失败”的接口，把业务 code 转成框架统一的 JiangResult<Unit>。
     */
    private fun ZdsBaseResult<*>.toUnitResult(): JiangResult<Unit> {
        if (isSuccess()) {
            return JiangResult.Success(Unit)
        }
        return JiangResult.Error(
            JiangException(
                code = code,
                message = message ?: msg ?: "Request failed.",
            ),
        )
    }

    private companion object {
        const val CLIENT_TYPE_ANDROID = "1"
        const val CODE_KEY_PART_ALREADY_BOUND = 10086
    }
}
