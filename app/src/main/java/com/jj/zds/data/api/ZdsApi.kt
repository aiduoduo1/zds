package com.jj.zds.data.api

import com.jj.zds.data.dto.aging.AgingProductDto
import com.jj.zds.data.dto.aging.AgingEndDetailDto
import com.jj.zds.data.dto.aging.AgingEndRequest
import com.jj.zds.data.dto.aging.AgingScanValidRequest
import com.jj.zds.data.dto.aging.AgingStartRequest
import com.jj.zds.data.dto.common.CurUserDto
import com.jj.zds.data.dto.common.UserSkillRequest
import com.jj.zds.data.dto.common.ZdsBaseResult
import com.jj.zds.data.dto.dimmer.DimmerBindingContextDto
import com.jj.zds.data.dto.dimmer.DimmerBindingRequest
import com.jj.zds.data.dto.device.OscilloscopeOptionDto
import com.jj.zds.data.dto.circulation.CirculationCombineRequest
import com.jj.zds.data.dto.circulation.CirculationCombineFromCheckRequest
import com.jj.zds.data.dto.circulation.CirculationDiscardRequest
import com.jj.zds.data.dto.circulation.CirculationProductCheckDto
import com.jj.zds.data.dto.circulation.CirculationProductCheckRequest
import com.jj.zds.data.dto.circulation.CirculationProductOperationRequest
import com.jj.zds.data.dto.circulation.CirculationQrCodeRequest
import com.jj.zds.data.dto.circulation.CirculationReasonDto
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
import com.jj.zds.data.dto.materialtrace.MaterialTraceBindDto
import com.jj.zds.data.dto.materialtrace.MaterialTraceInfoDto
import com.jj.zds.data.dto.materialtrace.MaterialTraceNodeDto
import com.jj.zds.data.dto.materialtrace.MaterialTraceQueryRequest
import com.jj.zds.data.dto.productionplan.LineDto
import com.jj.zds.data.dto.productionplan.PlanRequest
import com.jj.zds.data.dto.productionplan.ProductionBindProgressDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationInfoDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationInfoRequest
import com.jj.zds.data.dto.productionplan.ProductionCirculationMaterialDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationPrintLogDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationPrintRequest
import com.jj.zds.data.dto.productionplan.ProductionCirculationProgressDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationProgressRequest
import com.jj.zds.data.dto.productionplan.ProductionCirculationQueryRequest
import com.jj.zds.data.dto.productionplan.ProductionEndSwitchRequest
import com.jj.zds.data.dto.productionplan.ProductionEquipmentCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionFinishRequest
import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindRequest
import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindResultDto
import com.jj.zds.data.dto.productionplan.ProductionMaterialCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionOverallTestCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionOverallTestProgressDto
import com.jj.zds.data.dto.productionplan.ProductionOverallTestProgressRequest
import com.jj.zds.data.dto.productionplan.ProductionOverallTestSnInfoDto
import com.jj.zds.data.dto.productionplan.ProductionOverallTestSnInfoRequest
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
import com.jj.zds.data.dto.productstockin.ProductStockInRecordDto
import com.jj.zds.data.dto.productstockin.ProductStockInResponse
import com.jj.zds.data.dto.productstockin.ProductStockInRequest
import com.jj.zds.data.dto.productstockout.ProductStockOutChildTaskDto
import com.jj.zds.data.dto.productstockout.ProductStockOutChildTaskRequest
import com.jj.zds.data.dto.productstockout.ProductStockOutDetailDto
import com.jj.zds.data.dto.productstockout.ProductStockOutDetailRequest
import com.jj.zds.data.dto.productstockout.ProductStockOutFinishRequest
import com.jj.zds.data.dto.productstockout.ProductStockOutIdRequest
import com.jj.zds.data.dto.productstockout.ProductStockOutScanRequest
import com.jj.zds.data.dto.productstockout.ProductStockOutTaskDto
import com.jj.zds.data.dto.productreturn.AirportDto
import com.jj.zds.data.dto.productreturn.ProductReturnCreateRequest
import com.jj.zds.data.dto.productreturn.ProductReturnFinishRequest
import com.jj.zds.data.dto.productreturn.ProductReturnSaveRequest
import com.jj.zds.data.dto.productreturn.ProductReturnScanRequest
import com.jj.zds.data.dto.productreturn.ProductReturnScanResultDto
import com.jj.zds.data.dto.productreturn.ProductReturnTaskDto
import com.jj.zds.data.dto.qrcode.QrCodeDto
import com.jj.zds.data.dto.recheck.RecheckScanValidRequest
import com.jj.zds.data.dto.recheck.RecheckSubmitRequest
import com.jj.zds.data.dto.repair.RepairLogDto
import com.jj.zds.data.dto.repair.RepairLogListRequest
import com.jj.zds.data.dto.repair.NewRepairPartSnScanValidDto
import com.jj.zds.data.dto.repair.NewRepairPartSnScanValidRequest
import com.jj.zds.data.dto.repair.ProductRepairPartDto
import com.jj.zds.data.dto.repair.ProductRepairPartListRequest
import com.jj.zds.data.dto.repair.RepairOptionDto
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
import retrofit2.http.Query

interface ZdsApi {
    @POST("/device/dimmer-can/pda/binding/context") suspend fun dimmerBindingContext(@Body request: DimmerBindingRequest): ZdsBaseResult<DimmerBindingContextDto>
    @POST("/device/dimmer-can/pda/binding/bind") suspend fun bindDimmer(@Body request: DimmerBindingRequest): ZdsBaseResult<DimmerBindingContextDto>
    @POST("/device/dimmer-can/pda/binding/unbind") suspend fun unbindDimmer(@Body request: DimmerBindingRequest): ZdsBaseResult<Any>

    @POST("/m/trace/proInfo")
    suspend fun getMaterialTraceInfo(
        @Body request: MaterialTraceQueryRequest,
    ): ZdsBaseResult<MaterialTraceInfoDto>

    @POST("/m/trace/proBindInfo")
    suspend fun getMaterialTraceBindings(
        @Body request: MaterialTraceQueryRequest,
    ): ZdsBaseResult<List<MaterialTraceBindDto>>

    @POST("/m/trace/proNodeInfo")
    suspend fun getMaterialTraceNodes(
        @Body request: MaterialTraceQueryRequest,
    ): ZdsBaseResult<List<MaterialTraceNodeDto>>

    /**
     * 鐧诲綍锛屾垚鍔熷悗杩斿洖 mes-up-token銆?     */
    @Headers("No-Token: true")
    @POST("/ca/auth/login")
    suspend fun login(
        @Body request: LoginRequest,
    ): ZdsBaseResult<String>

    /**
     * 鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛淇℃伅銆?     */
    @POST("/s/sysUser/me")
    suspend fun initUser(): ZdsBaseResult<CurUserDto>

    @POST("/s/sysUser/one")
    suspend fun initUserByAccount(
        @Body request: UserSkillRequest,
    ): ZdsBaseResult<CurUserDto>

    /**
     * 鑾峰彇褰撳墠鐢ㄦ埛鑿滃崟銆?     */
    @POST("/s/sysMenu/myList")
    suspend fun getMenu(@Body map: Map<String, Int>): ZdsBaseResult<List<ZdsMenuDto>>

    /**
     * 妫€鏌ュ鎴风 APK 鐗堟湰銆?     */
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
     * 浜岀淮鐮佽В鏋愩€?     */
    @POST("/s/sysQrCode/decode")
    suspend fun decodeQrCode(@Body code: String): ZdsBaseResult<QrCodeDto>

    @POST("/device/external-device/online-oscilloscopes")
    suspend fun getOnlineOscilloscopes(): ZdsBaseResult<List<OscilloscopeOptionDto>>

    @POST("/device/micsig/screenAndGet")
    suspend fun captureOscilloscopeScreenRaw(@Query("deviceId") deviceId: Long): ResponseBody

    @POST("/q/ngSnRepairInfo/snScanValid")
    suspend fun scanValidRepairSn(
        @Body request: RepairSnScanValidRequest,
    ): ZdsBaseResult<RepairSnScanValidDto>

    /**
     * Debug plan statistics raw response.
     * New business APIs should prefer raw response and parse through ZdsRawApiCaller.
     */
    @POST("/m/iqcTask/statistics")
    suspend fun getDebugPlanStatisticsRaw(
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
    ): ResponseBody

    /**
     * 璋冭瘯璁″垝鍒楄〃銆?     */
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

    @POST("/pro/proAgingPlan/beScanValid")
    suspend fun scanValidAgingProduct(
        @Body request: AgingScanValidRequest,
    ): ZdsBaseResult<List<AgingProductDto>>

    @POST("/pro/proAgingPlan/startAging")
    suspend fun startAging(
        @Body request: AgingStartRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/proAgingPlan/afScanValid")
    suspend fun scanValidAgingEnd(
        @Body request: AgingScanValidRequest,
    ): ZdsBaseResult<AgingEndDetailDto>

    @POST("/pro/proAgingPlan/endAging")
    suspend fun endAging(
        @Body request: AgingEndRequest,
    ): ZdsBaseResult<Any>

    @POST("/fin/mat/pro/in-stock")
    suspend fun productStockIn(
        @Body request: ProductStockInRequest,
    ): ZdsBaseResult<ProductStockInResponse>

    @POST("/fin/finDeliveryTask/pda/list")
    suspend fun getProductStockOutTasks(
        @Body request: Map<String, String> = emptyMap(),
    ): ZdsBaseResult<List<ProductStockOutTaskDto>>

    @POST("/fin/finDeliveryCatDtl/pda/list")
    suspend fun getProductStockOutChildTasks(
        @Body request: ProductStockOutChildTaskRequest,
    ): ZdsBaseResult<ProductStockOutTaskDto>

    @POST("/fin/finDeliveryRes/pda/list")
    suspend fun getProductStockOutDetail(
        @Body request: ProductStockOutDetailRequest,
    ): ZdsBaseResult<ProductStockOutDetailDto>

    @POST("/fin/finDeliveryRes/pda/save")
    suspend fun submitProductStockOut(
        @Body request: ProductStockOutScanRequest,
    ): ZdsBaseResult<Map<String, List<String>>>

    @POST("/fin/finDeliveryRes/del")
    suspend fun deleteProductStockOutRecord(
        @Body request: ProductStockOutIdRequest,
    ): ZdsBaseResult<Any>

    @POST("/fin/finDeliveryCatDtl/finish")
    suspend fun finishProductStockOutChildTask(
        @Body request: ProductStockOutFinishRequest,
    ): ZdsBaseResult<Any>

    @POST("/fin/finDeliveryTask/finish")
    suspend fun finishProductStockOutTask(
        @Body request: ProductStockOutIdRequest,
    ): ZdsBaseResult<Any>

    @POST("/inf/air/list")
    suspend fun getReturnAirports(
        @Body request: Map<String, Boolean> = mapOf("isEnabled" to true),
    ): ZdsBaseResult<List<AirportDto>>

    @POST("/fin/finReturnTask/add")
    suspend fun createProductReturnTask(
        @Body request: ProductReturnCreateRequest,
    ): ZdsBaseResult<Any>

    @POST("/fin/finReturnTask/pda/list")
    suspend fun getProductReturnTasks(
        @Body request: Map<String, String> = emptyMap(),
    ): ZdsBaseResult<List<ProductReturnTaskDto>>

    @POST("/fin/finReturnDetail/pda/scan")
    suspend fun scanProductReturn(
        @Body request: ProductReturnScanRequest,
    ): ZdsBaseResult<ProductReturnScanResultDto>

    @POST("/fin/finReturnDetail/pda/save")
    suspend fun saveProductReturn(
        @Body request: ProductReturnSaveRequest,
    ): ZdsBaseResult<Any>

    @POST("/fin/finReturnTask/finish")
    suspend fun finishProductReturnTask(
        @Body request: ProductReturnFinishRequest,
    ): ZdsBaseResult<Any>

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

    @POST("/pro/proPlaDtl/testProgress")
    suspend fun getProductionOverallTestProgress(
        @Body request: ProductionOverallTestProgressRequest,
    ): ZdsBaseResult<ProductionOverallTestProgressDto>

    @POST("/pro/proPlaDtl/snTestInfo")
    suspend fun getProductionOverallTestSnInfo(
        @Body request: ProductionOverallTestSnInfoRequest,
    ): ZdsBaseResult<ProductionOverallTestSnInfoDto>

    @POST("/pro/proPlaDtl/check")
    suspend fun submitProductionOverallTest(
        @Body request: ProductionOverallTestCheckRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/matCirculation/circulationProgress")
    suspend fun getProductionCirculationProgress(
        @Body request: ProductionCirculationProgressRequest,
    ): ZdsBaseResult<ProductionCirculationProgressDto>

    @POST("/pro/matCirculation/circulationInfo")
    suspend fun getProductionCirculationInfo(
        @Body request: ProductionCirculationInfoRequest,
    ): ZdsBaseResult<ProductionCirculationInfoDto>

    @POST("/pro/matCirculationToMat/matInfo")
    suspend fun getProductionCirculationMaterials(
        @Body request: ProductionCirculationQueryRequest,
    ): ZdsBaseResult<List<ProductionCirculationMaterialDto>>

    @POST("/pro/matCirculationPrintLog/printLog")
    suspend fun getProductionCirculationPrintLogs(
        @Body request: ProductionCirculationQueryRequest,
    ): ZdsBaseResult<List<ProductionCirculationPrintLogDto>>

    @POST("/pro/matCirculation/print")
    suspend fun printProductionCirculation(
        @Body request: ProductionCirculationPrintRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/matCirculation/checkDiscard")
    suspend fun checkCirculationDiscard(
        @Body request: CirculationQrCodeRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/matCirculation/discardReason")
    suspend fun getCirculationDiscardReasons(): ZdsBaseResult<List<CirculationReasonDto>>

    @POST("/pro/matCirculation/discard")
    suspend fun discardCirculation(
        @Body request: CirculationDiscardRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/matCirculation/checkCombineMain")
    suspend fun checkCirculationCombineMain(
        @Body request: CirculationQrCodeRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/matCirculation/checkCombineFrom")
    suspend fun checkCirculationCombineFrom(
        @Body request: CirculationCombineFromCheckRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/matCirculation/combineReason")
    suspend fun getCirculationCombineReasons(): ZdsBaseResult<List<CirculationReasonDto>>

    @POST("/pro/matCirculation/combine")
    suspend fun combineCirculation(
        @Body request: CirculationCombineRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/matCirculation/checkAddCirculation")
    suspend fun checkAddProductCirculation(
        @Body request: CirculationQrCodeRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/matCirculation/checkAddPro")
    suspend fun checkCirculationAddProduct(
        @Body request: CirculationProductCheckRequest,
    ): ZdsBaseResult<CirculationProductCheckDto>

    @POST("/pro/matCirculation/addReason")
    suspend fun getCirculationAddReasons(): ZdsBaseResult<List<CirculationReasonDto>>

    @POST("/pro/matCirculation/addPro")
    suspend fun addCirculationProduct(
        @Body request: CirculationProductOperationRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/matCirculation/checkRemoveCirculation")
    suspend fun checkRemoveProductCirculation(
        @Body request: CirculationQrCodeRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/matCirculation/checkRemovePro")
    suspend fun checkCirculationRemoveProduct(
        @Body request: CirculationProductCheckRequest,
    ): ZdsBaseResult<CirculationProductCheckDto>

    @POST("/pro/matCirculation/removeReason")
    suspend fun getCirculationRemoveReasons(): ZdsBaseResult<List<CirculationReasonDto>>

    @POST("/pro/matCirculation/removePro")
    suspend fun removeCirculationProduct(
        @Body request: CirculationProductOperationRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/proPlaDtl/startProduce")
    suspend fun startProduction(
        @Body request: ProductionPlanExecuteRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/proPlaDtl/finishProduce")
    suspend fun finishProduction(
        @Body request: ProductionFinishRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/proPlaDtl/pauseProduce")
    suspend fun pauseProduction(
        @Body request: ProductionPlanExecuteRequest,
    ): ZdsBaseResult<Any>

    @POST("/pro/proPlaDtl/reStartProduce")
    suspend fun restartProduction(
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
     * 璋冭瘯璁″垝鏄庣粏锛屽寘鍚鍒掑熀纭€淇℃伅銆佹壒娆″垪琛ㄥ拰宸茬粦瀹氱鍗曘€?     */
    @POST("/m/iqcTaskItem/info")
    suspend fun getDebugPlanDetail(
        @Body request: DebugPlanDetailRequest,
    ): ZdsBaseResult<DebugPlanDetailDto>

    /**
     * 绠卞崟鎵弿鏍￠獙锛岀粦瀹氱鍗曞墠鏍￠獙绠卞崟浜岀淮鐮佸唴瀹广€?     */
    @POST("/m/iqcToBoxLabel/scanValid")
    suspend fun scanValidDebugBox(
        @Body request: DebugBoxScanValidRequest,
    ): ZdsBaseResult<DebugBoxScanValidDto>

    /**
     * 绠卞崟缁戝畾鎻愪氦锛屾妸鏍￠獙閫氳繃鐨勭鍗曚簩缁寸爜缁戝畾鍒拌皟璇曡鍒掋€?     */
    @POST("/m/iqcToBoxLabel/bind")
    suspend fun bindDebugBoxes(
        @Body request: DebugBoxBindRequest,
    ): ZdsBaseResult<Any>


    /**
     * SN 鎵弿鏍￠獙锛屾娊妫€鍓嶆牎楠?SN 浜岀淮鐮佸苟杩斿洖璋冭瘯妫€鏌ヨ〃銆?     */
    @POST("/m/iqcToSn/scanValid")
    suspend fun scanValidDebugSn(
        @Body request: DebugSnScanValidRequest,
    ): ZdsBaseResult<DebugSnScanValidDto>

    /**
     * 鎻愪氦 SN 璋冭瘯妫€鏌ョ粨鏋溿€?     */
    @POST("/m/iqcToSn/check")
    suspend fun submitDebugSnCheck(
        @Body request: DebugSnCheckRequest,
    ): ZdsBaseResult<Any>

    /**
     * 淇濆瓨璋冭瘯璁″垝鎿嶄綔鏃ュ織銆?     */
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
     * 鑾峰彇璋冭瘯鎵规淇℃伅銆?     */
    @POST("/m/iqcTaskItem/infoById")
    suspend fun getDebugTaskItemInfo(
        @Body request: DebugIdRequest,
    ): ZdsBaseResult<DebugTaskItemInfoDto>

    /**
     * 璋冭瘯鎵规瀹屽伐銆?     */
    @POST("/m/iqcTaskItem/finish")
    suspend fun finishDebugTaskItem(
        @Body request: DebugIdRequest,
    ): ZdsBaseResult<Any>

    /**
     * 璋冭瘯璁″垝瀹屽伐锛屽彲鎼哄甫鎷嶇収鍑瘉璧勬簮 ID锛屽繀瑕佹椂鏀寔寮哄埗瀹屽伐銆?     */
    @POST("/m/iqcTask/finish")
    suspend fun finishDebugPlan(
        @Body request: DebugPlanFinishRequest,
    ): ResponseBody

    /**
     * 鏍规嵁瀛楀吀缂栫爜鑾峰彇瀛楀吀椤癸紝璋冭瘯璁″垝閲岀敤浜庝笉鑹幇璞″垪琛ㄣ€?     */
    @POST("/s/sysDictItem/getByCode")
    suspend fun getDebugDictItems(
        @Body request: DebugDictRequest,
    ): ZdsBaseResult<List<DebugDictItemDto>>

    @POST("/s/sysDictItem/getByCode")
    suspend fun getRepairDictItems(
        @Body request: DebugDictRequest,
    ): ZdsBaseResult<List<DebugDictItemDto>>

    @POST("/q/ngProToMat/listByInfoId")
    suspend fun getProductRepairParts(
        @Body request: ProductRepairPartListRequest,
    ): ZdsBaseResult<List<ProductRepairPartDto>>

    @POST("/q/ngSnRepairInfo/maintainTypeOptions")
    suspend fun getRepairMaintainTypeOptions(): ZdsBaseResult<List<RepairOptionDto>>

    @POST("/q/ngSnRepairInfo/faultTypeOptions")
    suspend fun getRepairFaultTypeOptions(): ZdsBaseResult<List<RepairOptionDto>>

    @POST("/q/ngSnRepairInfo/newPartSnScanValid")
    suspend fun scanValidNewRepairPartSn(
        @Body request: NewRepairPartSnScanValidRequest,
    ): ZdsBaseResult<NewRepairPartSnScanValidDto>

    @POST("/q/ngSnRepairInfo/repair")
    suspend fun submitRepair(
        @Body request: RepairSubmitRequest,
    ): ZdsBaseResult<Any>

    @POST("/q/ngSnRepairLog/listByInfoId")
    suspend fun getRepairLogs(
        @Body request: RepairLogListRequest,
    ): ZdsBaseResult<List<RepairLogDto>>
}
