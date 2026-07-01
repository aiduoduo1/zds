package com.jj.zds.data.api

import com.jj.zds.data.dto.common.CurUserDto
import com.jj.zds.data.dto.common.UserSkillRequest
import com.jj.zds.data.dto.common.ZdsBaseResult
import com.jj.zds.data.dto.debugplan.DebugBoxBindRequest
import com.jj.zds.data.dto.debugplan.DebugBoxQrCodeDto
import com.jj.zds.data.dto.debugplan.DebugBoxQrCodeRequest
import com.jj.zds.data.dto.debugplan.DebugBoxScanValidDto
import com.jj.zds.data.dto.debugplan.DebugBoxScanValidRequest
import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import com.jj.zds.data.dto.debugplan.DebugDictRequest
import com.jj.zds.data.dto.debugplan.DebugIdRequest
import com.jj.zds.data.dto.debugplan.DebugPlanFinishRequest
import com.jj.zds.data.dto.debugplan.DebugPlanDetailDto
import com.jj.zds.data.dto.debugplan.DebugPlanDetailRequest
import com.jj.zds.data.dto.debugplan.DebugPlanDto
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
import com.jj.zds.data.dto.productionplan.ProductionMaterialCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionPersonCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionPlanDto
import com.jj.zds.data.dto.productionplan.ProductionPlanExecuteRequest
import com.jj.zds.data.dto.productionplan.ProductionPlanListRequest
import com.jj.zds.data.dto.productionplan.ProductionSnBindInfoDto
import com.jj.zds.data.dto.productionplan.ProductionSnBindInfoRequest
import com.jj.zds.data.dto.productionplan.ProductionSoftCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionStationProgressDetailDto
import com.jj.zds.data.dto.productionplan.ProductionSwitchBeginRequest
import com.jj.zds.data.dto.productionplan.ProductionSwitchDetailDto
import com.jj.zds.data.dto.productionplan.ProductionSwitchDetailRequest
import com.jj.zds.data.dto.qrcode.QrCodeDto
import com.jj.zds.data.dto.recheck.RecheckScanValidRequest
import com.jj.zds.data.dto.recheck.RecheckSubmitRequest
import com.jj.zds.data.dto.repair.RepairLogDto
import com.jj.zds.data.dto.repair.RepairLogListRequest
import com.jj.zds.data.dto.repair.RepairSnScanValidDto
import com.jj.zds.data.dto.repair.RepairSnScanValidRequest
import com.jj.zds.data.dto.repair.RepairSubmitRequest
import com.jj.zds.data.dto.update.ZdsApkVersionDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ZdsApi {

    /**
     * 登录，成功后返回 mes-up-token。
     */
    @Headers("No-Token: true")
    @POST("/ca/auth/login")
    suspend fun login(
        @Body request: LoginRequest,
    ): ZdsBaseResult<String>

    /**
     * 获取当前登录用户信息。
     */
    @POST("/s/sysUser/me")
    suspend fun initUser(): ZdsBaseResult<CurUserDto>

    @POST("/s/sysUser/one")
    suspend fun initUserByAccount(
        @Body request: UserSkillRequest,
    ): ZdsBaseResult<CurUserDto>

    /**
     * 获取当前用户菜单。
     */
    @POST("/s/sysMenu/myList")
    suspend fun getMenu(@Body map: Map<String, Int>): ZdsBaseResult<List<ZdsMenuDto>>

    /**
     * 检查客户端 APK 版本。
     */
    @POST("/s/sysClientVersion/versionCheck")
    suspend fun versionCheck(
        @Body map: Map<String, String?>,
    ): ZdsBaseResult<ZdsApkVersionDto?>

    @Multipart
    @POST("/task/taskFile/upload")
    suspend fun upload(
        @Part list: List<MultipartBody.Part>,
    ): ResponseBody

    /**
     * 二维码解析。
     */
    @POST("/s/sysQrCode/decode")
    suspend fun decodeQrCode(@Body code: String): ZdsBaseResult<QrCodeDto>

    @POST("/q/ngSnRepairInfo/snScanValid")
    suspend fun scanValidRepairSn(
        @Body request: RepairSnScanValidRequest,
    ): ZdsBaseResult<RepairSnScanValidDto>

    /**
     * 未完成调试计划统计。
     */
    @POST("/m/iqcTask/statistics")
    suspend fun getDebugPlanStatistics(
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
    ): ZdsBaseResult<DebugPlanStatisticsDto>

    /**
     * 调试计划列表。
     */
    @POST("/m/iqcTask/list")
    suspend fun getDebugPlanList(
        @Body request: DebugPlanListRequest,
    ): ZdsBaseResult<List<DebugPlanDto>>

    @POST("/pla/pla/pdaPlanList")
    suspend fun getProductionPlanList(
        @Body request: ProductionPlanListRequest,
    ): ZdsBaseResult<List<ProductionPlanDto>>

    @POST("/pla/pla/lineCount")
    suspend fun getProductionLineCount(
        @Body request: PlanRequest,
    ): ZdsBaseResult<List<LineDto>>

    @POST("/pro/proPlaDtl/bindProgress")
    suspend fun getProductionBindProgress(
        @Body request: ProductionPlanExecuteRequest,
    ): ZdsBaseResult<ProductionBindProgressDto>

    @POST("/pro/proPlaDtl/snBindInfo")
    suspend fun getProductionSnBindInfo(
        @Body request: ProductionSnBindInfoRequest,
    ): ZdsBaseResult<ProductionSnBindInfoDto>

    @POST("/pro/proPlaDtl/bind")
    suspend fun bindProductionKeyPart(
        @Body request: ProductionKeyPartBindRequest,
    ): ZdsBaseResult<ProductionKeyPartBindResultDto>

    @POST("/pro/proPlaDtl/staList")
    suspend fun getProductionStationProgressList(
        @Body request: ProductionPlanExecuteRequest,
    ): ZdsBaseResult<ProductionStationProgressDetailDto>

    @POST("/pro/proPlaDtl/startProduce")
    suspend fun startProduction(
        @Body request: ProductionPlanExecuteRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/proLineSwi/beginSwitch")
    suspend fun beginProductionSwitch(
        @Body request: ProductionSwitchBeginRequest,
    ): ZdsBaseResult<Int>

    @POST("/pro/proLineSwi/pdaSwitchDetail")
    suspend fun getProductionSwitchDetail(
        @Body request: ProductionSwitchDetailRequest,
    ): ZdsBaseResult<ProductionSwitchDetailDto>

    @POST("/pro/proLineSwiSof/confirmSoftCheck")
    suspend fun confirmProductionSoftCheck(
        @Body request: ProductionSoftCheckRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/proLineSwiCat/confirmMatCheck")
    suspend fun confirmProductionMaterialCheck(
        @Body request: ProductionMaterialCheckRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/proLineSwiUser/confirmUserCheck")
    suspend fun confirmProductionPersonCheck(
        @Body request: List<ProductionPersonCheckRequest>,
    ): ZdsBaseResult<Any>

    @POST("/pro/proLineSwiEqu/confirmEquCheck")
    suspend fun confirmProductionEquipmentCheck(
        @Body request: List<ProductionEquipmentCheckRequest>,
    ): ZdsBaseResult<Any>

    @POST("/pro/proLineSwi/endSwitch")
    suspend fun endProductionSwitch(
        @Body request: ProductionEndSwitchRequest,
    ): ZdsBaseResult<Any>

    /**
     * 调试计划明细，包含计划基础信息、批次列表和已绑定箱单。
     */
    @POST("/m/iqcTaskItem/info")
    suspend fun getDebugPlanDetail(
        @Body request: DebugPlanDetailRequest,
    ): ZdsBaseResult<DebugPlanDetailDto>

    /**
     * 箱单扫描校验，绑定箱单前校验箱单二维码内容。
     */
    @POST("/m/iqcToBoxLabel/scanValid")
    suspend fun scanValidDebugBox(
        @Body request: DebugBoxScanValidRequest,
    ): ZdsBaseResult<DebugBoxScanValidDto>

    /**
     * 箱单绑定提交，把校验通过的箱单二维码绑定到调试计划。
     */
    @POST("/m/iqcToBoxLabel/bind")
    suspend fun bindDebugBoxes(
        @Body request: DebugBoxBindRequest,
    ): ZdsBaseResult<Any>


    /**
     * SN 扫描校验，抽检前校验 SN 二维码并返回调试检查表。
     */
    @POST("/m/iqcToSn/scanValid")
    suspend fun scanValidDebugSn(
        @Body request: DebugSnScanValidRequest,
    ): ZdsBaseResult<DebugSnScanValidDto>

    /**
     * 提交 SN 调试检查结果。
     */
    @POST("/m/iqcToSn/check")
    suspend fun submitDebugSnCheck(
        @Body request: DebugSnCheckRequest,
    ): ZdsBaseResult<Any>

    /**
     * 保存调试计划操作日志。
     */
    @POST("/m/iqcTaskOpLog/addLog")
    suspend fun saveDebugTaskLog(
        @Body request: DebugTaskLogRequest,
    ): ZdsBaseResult<Any>

    @POST("/m/iqcToSn/singleCheckValid")
    suspend fun scanValidSingleDebugSn(
        @Body request: DebugBoxQrCodeRequest,
    ): ZdsBaseResult<DebugSnScanValidDto>

    @POST("/m/iqcToSn/singleCheck")
    suspend fun submitSingleDebugCheck(
        @Body request: SingleDebugCheckRequest,
    ): ZdsBaseResult<Any>

    @POST("/m/iqcToSn/recheckValid")
    suspend fun scanValidRecheckSn(
        @Body request: RecheckScanValidRequest,
    ): ZdsBaseResult<DebugSnScanValidDto>

    @POST("/m/iqcToSn/recheck")
    suspend fun submitRecheck(
        @Body request: RecheckSubmitRequest,
    ): ZdsBaseResult<Any>

    /**
     * 获取调试批次信息。
     */
    @POST("/m/iqcTaskItem/infoById")
    suspend fun getDebugTaskItemInfo(
        @Body request: DebugIdRequest,
    ): ZdsBaseResult<DebugTaskItemInfoDto>

    /**
     * 调试批次完工。
     */
    @POST("/m/iqcTaskItem/finish")
    suspend fun finishDebugTaskItem(
        @Body request: DebugIdRequest,
    ): ZdsBaseResult<Any>

    /**
     * 调试计划完工，可携带拍照凭证资源 ID，必要时支持强制完工。
     */
    @POST("/m/iqcTask/finish")
    suspend fun finishDebugPlan(
        @Body request: DebugPlanFinishRequest,
    ): ResponseBody

    /**
     * 根据字典编码获取字典项，调试计划里用于不良现象列表。
     */
    @POST("/s/sysDictItem/getByCode")
    suspend fun getDebugDictItems(
        @Body request: DebugDictRequest,
    ): ZdsBaseResult<List<DebugDictItemDto>>

    @POST("/s/sysDictItem/getByCode")
    suspend fun getRepairDictItems(
        @Body request: DebugDictRequest,
    ): ZdsBaseResult<List<DebugDictItemDto>>

    @POST("/q/ngSnRepairInfo/repair")
    suspend fun submitRepair(
        @Body request: RepairSubmitRequest,
    ): ZdsBaseResult<Any>

    @POST("/q/ngSnRepairLog/listByInfoId")
    suspend fun getRepairLogs(
        @Body request: RepairLogListRequest,
    ): ZdsBaseResult<List<RepairLogDto>>
}
