package com.jj.zds.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.common.result.JiangResult
import com.jj.zds.data.dto.debugplan.DebugBoxQrCodeDto
import com.jj.zds.data.dto.common.CurUserDto
import com.jj.zds.data.dto.dimmer.DimmerBindingContextDto
import com.jj.zds.data.dto.dimmer.DimmerBindingRequest
import com.jj.zds.data.dto.device.OscilloscopeOptionDto
import com.jj.zds.data.dto.debugplan.DebugBoxScanValidDto
import com.jj.zds.data.dto.debugplan.DebugCheckGroupDto
import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import com.jj.zds.data.dto.debugplan.DebugPlanDetailDto
import com.jj.zds.data.dto.debugplan.DebugPlanDto
import com.jj.zds.data.dto.debugplan.DebugPlanListRequest
import com.jj.zds.data.dto.debugplan.DebugPlanStatisticsDto
import com.jj.zds.data.dto.debugplan.DebugSnScanValidDto
import com.jj.zds.data.dto.debugplan.DebugTaskItemInfoDto
import com.jj.zds.data.dto.debugplan.DebugTaskLogRequest
import com.jj.zds.data.dto.login.LoginRequest
import com.jj.zds.data.dto.menu.ZdsMenuDto
import com.jj.zds.data.dto.productionplan.LineDto
import com.jj.zds.data.dto.productionplan.ProductionBindProgressDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationInfoDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationMaterialDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationPrintLogDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationPrintRequest
import com.jj.zds.data.dto.productionplan.ProductionCirculationProgressDto
import com.jj.zds.data.dto.productionplan.ProductionEquipmentCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindRequest
import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindResultDto
import com.jj.zds.data.dto.productionplan.ProductionOverallTestProgressDto
import com.jj.zds.data.dto.productionplan.ProductionOverallTestSnInfoDto
import com.jj.zds.data.dto.productionplan.ProductionPlanDto
import com.jj.zds.data.dto.productionplan.ProductionPlanStatus
import com.jj.zds.data.dto.productionplan.ProductionPersonCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionSnBindInfoDto
import com.jj.zds.data.dto.productionplan.ProductionStationProgressDto
import com.jj.zds.data.dto.productionplan.ProductionSwitchDetailDto
import com.jj.zds.data.dto.qrcode.QrCodeDto
import com.jj.zds.data.dto.repair.RepairAction
import com.jj.zds.data.dto.repair.RepairLogDto
import com.jj.zds.data.dto.repair.NewRepairPartSnScanValidDto
import com.jj.zds.data.dto.repair.ProductRepairPartDto
import com.jj.zds.data.dto.repair.RepairOptionDto
import com.jj.zds.data.dto.repair.RepairSnScanValidDto
import com.jj.zds.data.dto.repair.RepairSubmitRequest
import com.jj.zds.data.dto.update.ZdsVersionCheckResult
import com.jj.zds.data.repository.ZdsRepository
import java.io.File
import kotlinx.coroutines.launch

data class DebugTaskCheckPhotoUploadResult(
    val checkId: String,
    val fileId: Long,
)

data class DebugTaskLogSaveResult(
    val request: DebugTaskLogRequest,
    val success: Boolean,
    val message: String = "",
)

data class DebugTaskNgLimitPlanResult(
    val taskId: String,
    val detail: DebugPlanDetailDto? = null,
    val message: String = "",
)

data class ProductionSwitchBeginResult(
    val planId: String,
    val switchId: Int,
)

data class ProductionStartResult(
    val planId: String,
)

data class ProductionRunStateChangeResult(
    val planId: String,
    val paused: Boolean,
)

class ZdsViewModel : JiangViewModel() {

    private val repository by lazy { ZdsRepository() }
    private val _dimmerBindingContext = MutableLiveData<DimmerBindingContextDto>()
    val dimmerBindingContext: LiveData<DimmerBindingContextDto> = _dimmerBindingContext
    private val _dimmerBindingDone = MutableLiveData<Boolean>()
    val dimmerBindingDone: LiveData<Boolean> = _dimmerBindingDone
    private val _dimmerAirports = MutableLiveData<List<com.jj.zds.data.dto.productreturn.AirportDto>>()
    val dimmerAirports: LiveData<List<com.jj.zds.data.dto.productreturn.AirportDto>> = _dimmerAirports

    fun loadDimmerAirports() = launchResult(loadingMode = JiangLoadingMode.NONE, block = { repository.getReturnAirports() }, onSuccess = { _dimmerAirports.value = it })
    fun loadDimmerBinding(stationQr: String, proQr: String? = null) = launchResult(loadingMode = JiangLoadingMode.DIALOG, block = { repository.dimmerBindingContext(DimmerBindingRequest(stationQr, proQr)) }, onSuccess = { _dimmerBindingContext.value = it })
    fun bindDimmer(stationQr: String, proQr: String, airportName: String, projectName: String) = launchResult(loadingMode = JiangLoadingMode.DIALOG, block = { repository.bindDimmer(DimmerBindingRequest(stationQr, proQr, airportName, projectName)) }, onSuccess = { _dimmerBindingContext.value = it; _dimmerBindingDone.value = true })
    fun unbindDimmer(stationQr: String, proQr: String) = launchResult(loadingMode = JiangLoadingMode.DIALOG, block = { repository.unbindDimmer(DimmerBindingRequest(stationQr, proQr)) }, onSuccess = { _dimmerBindingDone.value = false })

    private val _loginSuccess = MutableLiveData<CurUserDto>()
    val loginSuccess: LiveData<CurUserDto> = _loginSuccess

    private val _userByAccount = MutableLiveData<CurUserDto>()
    val userByAccount: LiveData<CurUserDto> = _userByAccount

    private val _menus = MutableLiveData<List<ZdsMenuDto>>()
    val menus: LiveData<List<ZdsMenuDto>> = _menus

    private val _versionCheckResult = MutableLiveData<ZdsVersionCheckResult>()
    val versionCheckResult: LiveData<ZdsVersionCheckResult> = _versionCheckResult

    private val _apkDownloadProgress = MutableLiveData<Int>()
    val apkDownloadProgress: LiveData<Int> = _apkDownloadProgress

    private val _apkDownloadResult = MutableLiveData<File>()
    val apkDownloadResult: LiveData<File> = _apkDownloadResult

    private val _qrCodeDecodeResult = MutableLiveData<QrCodeDto>()
    val qrCodeDecodeResult: LiveData<QrCodeDto> = _qrCodeDecodeResult

    private val _oscilloscopeScreenshotFileId = MutableLiveData<Long>()
    val oscilloscopeScreenshotFileId: LiveData<Long> = _oscilloscopeScreenshotFileId
    private val _onlineOscilloscopes = MutableLiveData<List<OscilloscopeOptionDto>?>()
    val onlineOscilloscopes: LiveData<List<OscilloscopeOptionDto>?> = _onlineOscilloscopes

    private val _repairSnScanValidResult = MutableLiveData<RepairSnScanValidDto>()
    val repairSnScanValidResult: LiveData<RepairSnScanValidDto> = _repairSnScanValidResult

    private val _repairNgReasonItems = MutableLiveData<List<DebugDictItemDto>>()
    val repairNgReasonItems: LiveData<List<DebugDictItemDto>> = _repairNgReasonItems

    private val _repairResultItems = MutableLiveData<List<DebugDictItemDto>>()
    val repairResultItems: LiveData<List<DebugDictItemDto>> = _repairResultItems

    private val _repairOperationDone = MutableLiveData<Unit>()
    val repairOperationDone: LiveData<Unit> = _repairOperationDone

    private val _repairLogs = MutableLiveData<List<RepairLogDto>>()
    val repairLogs: LiveData<List<RepairLogDto>> = _repairLogs

    private val _productRepairParts = MutableLiveData<List<ProductRepairPartDto>>()
    val productRepairParts: LiveData<List<ProductRepairPartDto>> = _productRepairParts

    private val _repairMaintainTypeOptions = MutableLiveData<List<RepairOptionDto>>()
    val repairMaintainTypeOptions: LiveData<List<RepairOptionDto>> = _repairMaintainTypeOptions

    private val _repairFaultTypeOptions = MutableLiveData<List<RepairOptionDto>>()
    val repairFaultTypeOptions: LiveData<List<RepairOptionDto>> = _repairFaultTypeOptions

    private val _newRepairPartSnScanValidResult = MutableLiveData<NewRepairPartSnScanValidDto>()
    val newRepairPartSnScanValidResult: LiveData<NewRepairPartSnScanValidDto> = _newRepairPartSnScanValidResult

    private val _repairPhotoUploadResult = MutableLiveData<Long>()
    val repairPhotoUploadResult: LiveData<Long> = _repairPhotoUploadResult

    private val _debugPlanStatistics = MutableLiveData<DebugPlanStatisticsDto>()
    val debugPlanStatistics: LiveData<DebugPlanStatisticsDto> = _debugPlanStatistics

    private val _debugPlans = MutableLiveData<List<DebugPlanDto>>()
    val debugPlans: LiveData<List<DebugPlanDto>> = _debugPlans

    private val _debugPlanProgressPlans = MutableLiveData<List<DebugPlanDto>>()
    val debugPlanProgressPlans: LiveData<List<DebugPlanDto>> = _debugPlanProgressPlans

    private val _debugPlansLoadFinished = MutableLiveData<Unit>()
    val debugPlansLoadFinished: LiveData<Unit> = _debugPlansLoadFinished

    private val _productionPlans = MutableLiveData<List<ProductionPlanDto>>()
    val productionPlans: LiveData<List<ProductionPlanDto>> = _productionPlans

    private val _productionPlansLoadFinished = MutableLiveData<Unit>()
    val productionPlansLoadFinished: LiveData<Unit> = _productionPlansLoadFinished

    private val _productionLineCounts = MutableLiveData<List<LineDto>>()
    val productionLineCounts: LiveData<List<LineDto>> = _productionLineCounts

    private val _productionStationProgressList = MutableLiveData<List<ProductionStationProgressDto>>()
    val productionStationProgressList: LiveData<List<ProductionStationProgressDto>> =
        _productionStationProgressList

    private val _productionOverallTestProgress = MutableLiveData<ProductionOverallTestProgressDto>()
    val productionOverallTestProgress: LiveData<ProductionOverallTestProgressDto> =
        _productionOverallTestProgress

    private val _productionOverallTestSnInfo = MutableLiveData<ProductionOverallTestSnInfoDto>()
    val productionOverallTestSnInfo: LiveData<ProductionOverallTestSnInfoDto> =
        _productionOverallTestSnInfo

    private val _productionOverallTestOperationDone = MutableLiveData<Boolean>()
    val productionOverallTestOperationDone: LiveData<Boolean> =
        _productionOverallTestOperationDone

    private val _productionBindProgress = MutableLiveData<ProductionBindProgressDto>()
    val productionBindProgress: LiveData<ProductionBindProgressDto> = _productionBindProgress

    private val _productionCirculationProgress = MutableLiveData<ProductionCirculationProgressDto>()
    val productionCirculationProgress: LiveData<ProductionCirculationProgressDto> =
        _productionCirculationProgress

    private val _productionCirculationInfo = MutableLiveData<ProductionCirculationInfoDto>()
    val productionCirculationInfo: LiveData<ProductionCirculationInfoDto> =
        _productionCirculationInfo

    private val _productionCirculationMaterials = MutableLiveData<List<ProductionCirculationMaterialDto>>()
    val productionCirculationMaterials: LiveData<List<ProductionCirculationMaterialDto>> =
        _productionCirculationMaterials

    private val _productionCirculationPrintLogs = MutableLiveData<List<ProductionCirculationPrintLogDto>>()
    val productionCirculationPrintLogs: LiveData<List<ProductionCirculationPrintLogDto>> =
        _productionCirculationPrintLogs

    private val _productionCirculationPrintDone = MutableLiveData<Unit>()
    val productionCirculationPrintDone: LiveData<Unit> = _productionCirculationPrintDone

    private val _productionSnBindInfo = MutableLiveData<ProductionSnBindInfoDto>()
    val productionSnBindInfo: LiveData<ProductionSnBindInfoDto> = _productionSnBindInfo

    private val _productionKeyPartBindResult = MutableLiveData<ProductionKeyPartBindResultDto>()
    val productionKeyPartBindResult: LiveData<ProductionKeyPartBindResultDto> =
        _productionKeyPartBindResult

    private val _productionSwitchBeginResult = MutableLiveData<ProductionSwitchBeginResult>()
    val productionSwitchBeginResult: LiveData<ProductionSwitchBeginResult> = _productionSwitchBeginResult

    private val _productionStartResult = MutableLiveData<ProductionStartResult>()
    val productionStartResult: LiveData<ProductionStartResult> = _productionStartResult

    private val _productionFinishDone = MutableLiveData<Unit>()
    val productionFinishDone: LiveData<Unit> = _productionFinishDone

    private val _productionRunStateChanged = MutableLiveData<ProductionRunStateChangeResult>()
    val productionRunStateChanged: LiveData<ProductionRunStateChangeResult> =
        _productionRunStateChanged

    private val _productionSwitchDetail = MutableLiveData<ProductionSwitchDetailDto>()
    val productionSwitchDetail: LiveData<ProductionSwitchDetailDto> = _productionSwitchDetail

    private val _productionCheckDone = MutableLiveData<Unit>()
    val productionCheckDone: LiveData<Unit> = _productionCheckDone

    private val _productionSwitchEnded = MutableLiveData<Unit>()
    val productionSwitchEnded: LiveData<Unit> = _productionSwitchEnded

    private val _debugPlanDetail = MutableLiveData<DebugPlanDetailDto>()
    val debugPlanDetail: LiveData<DebugPlanDetailDto> = _debugPlanDetail

    private val _debugBoxScanValidResult = MutableLiveData<DebugBoxScanValidDto>()
    val debugBoxScanValidResult: LiveData<DebugBoxScanValidDto> = _debugBoxScanValidResult

    private val _debugBoxQrCodeResult = MutableLiveData<DebugBoxQrCodeDto>()
    val debugBoxQrCodeResult: LiveData<DebugBoxQrCodeDto> = _debugBoxQrCodeResult

    private val _debugSnScanValidResult = MutableLiveData<DebugSnScanValidDto>()
    val debugSnScanValidResult: LiveData<DebugSnScanValidDto> = _debugSnScanValidResult

    private val _singleDebugSnScanValidResult = MutableLiveData<DebugSnScanValidDto>()
    val singleDebugSnScanValidResult: LiveData<DebugSnScanValidDto> = _singleDebugSnScanValidResult

    private val _recheckSnScanValidResult = MutableLiveData<DebugSnScanValidDto>()
    val recheckSnScanValidResult: LiveData<DebugSnScanValidDto> = _recheckSnScanValidResult

    private val _debugTaskItemInfo = MutableLiveData<DebugTaskItemInfoDto>()
    val debugTaskItemInfo: LiveData<DebugTaskItemInfoDto> = _debugTaskItemInfo

    private val _debugTaskLogSaveResult = MutableLiveData<DebugTaskLogSaveResult>()
    val debugTaskLogSaveResult: LiveData<DebugTaskLogSaveResult> = _debugTaskLogSaveResult

    private val _debugTaskNgLimitPlanResult = MutableLiveData<DebugTaskNgLimitPlanResult>()
    val debugTaskNgLimitPlanResult: LiveData<DebugTaskNgLimitPlanResult> =
        _debugTaskNgLimitPlanResult

    private val _debugPlanFinishResult = MutableLiveData<Map<String, String>?>()
    val debugPlanFinishResult: LiveData<Map<String, String>?> = _debugPlanFinishResult

    private val _debugPlanPhotoUploadResult = MutableLiveData<Long>()
    val debugPlanPhotoUploadResult: LiveData<Long> = _debugPlanPhotoUploadResult

    private val _debugTaskCheckPhotoUploadResult = MutableLiveData<DebugTaskCheckPhotoUploadResult>()
    val debugTaskCheckPhotoUploadResult: LiveData<DebugTaskCheckPhotoUploadResult> =
        _debugTaskCheckPhotoUploadResult

    private val _debugDictItems = MutableLiveData<List<DebugDictItemDto>>()
    val debugDictItems: LiveData<List<DebugDictItemDto>> = _debugDictItems

    private val _debugOperationDone = MutableLiveData<Unit>()
    val debugOperationDone: LiveData<Unit> = _debugOperationDone

    fun hasToken(): Boolean = repository.hasToken()

    /**
     * 登录入口：先做页面侧空值校验，再交给 Repository 完成 token + 用户信息初始化。
     */
    fun login(account: String, password: String) {
        val trimmedAccount = account.trim()
        val trimmedPassword = password.trim()
        if (trimmedAccount.isBlank()) {
            showToast("请输入账号")
            return
        }
        if (trimmedPassword.isBlank()) {
            showToast("请输入密码")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "登录中...",
            block = {
                repository.loginAndInitUser(
                    LoginRequest(
                        account = trimmedAccount,
                        password = trimmedPassword,
                    ),
                )
            },
            onSuccess = { user ->
                _loginSuccess.value = user
            },
        )
    }

    fun loadMenu() {
        launchResult(
            loadingMode = JiangLoadingMode.PAGE,
            block = { repository.getMenu() },
            onSuccess = { menus ->
                _menus.value = menus
            },
        )
    }

    fun clearLogin() {
        repository.clearLogin()
    }

    fun checkVersion(versionName: String) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "Checking version...",
            block = { repository.versionCheck(versionName) },
            onSuccess = { result ->
                _versionCheckResult.value = result
            },
        )
    }

    /**
     * 下载更新包时手动维护进度 LiveData，因为下载回调不走普通 launchResult 成功链。
     */
    fun downloadUpdateApk(fileId: String, targetFile: File) {
        _apkDownloadProgress.value = 0
        launch(
            showLoading = false,
            block = {
                when (
                    val result = repository.downloadUpdateApk(
                        fileId = fileId,
                        targetFile = targetFile,
                        onProgress = { progress ->
                            _apkDownloadProgress.postValue(progress)
                        },
                    )
                ) {
                    is JiangResult.Success -> {
                        _apkDownloadProgress.value = 100
                        _apkDownloadResult.value = result.data
                    }

                    is JiangResult.Error -> {
                        throw result.exception
                    }

                    JiangResult.Loading,
                    JiangResult.Empty,
                    -> Unit
                }
            },
        )
    }

    /**
     * 通用二维码解析入口，页面只需要观察 qrCodeDecodeResult。
     */
    fun decodeQrCode(code: String) {
        val trimmedCode = code.trim()
        if (trimmedCode.isBlank()) {
            showToast("请先扫码")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "二维码解析中...",
            block = { repository.decodeQrCode(trimmedCode) },
            onSuccess = { result ->
                _qrCodeDecodeResult.value = result
            },
        )
    }

    fun captureOscilloscopeScreen() {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "正在查询可用示波器...",
            block = { repository.getOnlineOscilloscopes() },
            onSuccess = { devices -> _onlineOscilloscopes.value = devices },
        )
    }

    fun captureOscilloscopeScreen(deviceId: Long) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "正在获取示波器截图...",
            block = { repository.captureOscilloscopeScreen(deviceId) },
            onSuccess = { fileId ->
                _oscilloscopeScreenshotFileId.value = fileId
            },
        )
    }

    fun consumeOscilloscopeChoices() {
        _onlineOscilloscopes.value = null
    }

    fun scanValidRepairSn(qrCode: String) {
        val trimmedCode = qrCode.trim()
        if (trimmedCode.isBlank()) {
            showToast("请先扫码")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.scanValidRepairSn(trimmedCode) },
            onSuccess = { result ->
                _repairSnScanValidResult.value = result
            },
        )
    }

    fun loadRepairNgReasons() {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getRepairNgReasonItems() },
            onSuccess = { result ->
                _repairNgReasonItems.value = result
            },
        )
    }

    fun loadRepairResults() {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getRepairResultItems() },
            onSuccess = { result ->
                _repairResultItems.value = result
            },
        )
    }

    fun loadProductRepairParts(infoId: Long) {
        if (infoId <= 0L) {
            showToast("不良上报ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getProductRepairParts(infoId) },
            onSuccess = { result ->
                _productRepairParts.value = result
            },
        )
    }

    fun loadRepairMaintainTypeOptions() {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getRepairMaintainTypeOptions() },
            onSuccess = { result ->
                _repairMaintainTypeOptions.value = result
            },
        )
    }

    fun loadRepairFaultTypeOptions() {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getRepairFaultTypeOptions() },
            onSuccess = { result ->
                _repairFaultTypeOptions.value = result
            },
        )
    }

    fun scanValidNewRepairPartSn(damagePartCatId: String, qrCode: String) {
        val trimmedCatId = damagePartCatId.trim()
        val trimmedCode = qrCode.trim()
        when {
            trimmedCatId.isBlank() -> {
                showToast("请先选择损坏关键部件")
                return
            }

            trimmedCode.isBlank() -> {
                showToast("请先扫描新关键部件 SN 码")
                return
            }
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.scanValidNewRepairPartSn(trimmedCatId, trimmedCode) },
            onSuccess = { result ->
                _newRepairPartSnScanValidResult.value = result
            },
        )
    }

    fun submitRepair(request: RepairSubmitRequest) {
        if (request.id <= 0L) {
            showToast("不良上报ID为空")
            return
        }
        val requireCompleteInput = request.action == RepairAction.SUBMIT.value
        if (requireCompleteInput && request.result.isBlank()) {
            showToast("请选择维修结果")
            return
        }
        if (requireCompleteInput && request.ngReason.isBlank()) {
            showToast("请选择不良原因")
            return
        }
        if (requireCompleteInput && request.repairBy.isBlank()) {
            showToast("请填写维修人")
            return
        }
        if (requireCompleteInput && request.repairDesc.isBlank()) {
            showToast("请填写维修说明")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.submitRepair(request) },
            onSuccess = {
                _repairOperationDone.value = Unit
            },
        )
    }

    fun loadRepairLogs(infoId: Long) {
        if (infoId <= 0L) {
            showToast("不良上报ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getRepairLogs(infoId) },
            onSuccess = { result ->
                _repairLogs.value = result
            },
        )
    }

    fun uploadRepairPhoto(file: File) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "上传维修照片中...",
            block = { repository.uploadFile(file) },
            onSuccess = { fileId ->
                _repairPhotoUploadResult.value = fileId
            },
        )
    }

    fun loadDebugPlanStatistics() {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getDebugPlanStatistics() },
            onSuccess = { statistics ->
                _debugPlanStatistics.value = statistics
            },
        )
    }

    /**
     * 调试计划列表加载：搜索词和状态筛选直接透传给后端，页面再做本地展示过滤。
     */
    fun loadDebugPlans(
        categoryName: String = "",
        code: String = "",
        status: Int? = null,
    ) {
        launch(showLoading = false) {
            try {
                when (
                    val result = repository.getDebugPlanList(
                    DebugPlanListRequest(
                        categoryName = categoryName,
                        code = code,
                        status = status,
                    ),
                )
                ) {
                    is JiangResult.Success -> {
                        _debugPlans.value = result.data
                        setSuccess(result.data)
                    }
                    is JiangResult.Error -> setError(
                        code = result.exception.code,
                        message = result.exception.message,
                        throwable = result.exception.throwable,
                    )
                    JiangResult.Loading -> Unit
                    JiangResult.Empty -> {
                        _debugPlans.value = emptyList()
                        setEmpty()
                    }
                }
            } finally {
                _debugPlansLoadFinished.value = Unit
            }
        }
    }

    fun loadProductionPlans(
        keyword: String = "",
        lineId: String? = null,
    ) {
        launch(showLoading = false) {
            try {
                when (
                    val result = repository.getProductionPlanList(
                        keyword = keyword.trim(),
                        lineId = lineId,
                    )
                ) {
                    is JiangResult.Success -> {
                        _productionPlans.value = result.data
                        setSuccess(result.data)
                    }
                    is JiangResult.Error -> setError(
                        code = result.exception.code,
                        message = result.exception.message,
                        throwable = result.exception.throwable,
                    )
                    JiangResult.Loading -> Unit
                    JiangResult.Empty -> {
                        _productionPlans.value = emptyList()
                        setEmpty()
                    }
                }
            } finally {
                _productionPlansLoadFinished.value = Unit
            }
        }
    }

    fun loadProductionLineCounts(
        planStatusList: ArrayList<Int> = ProductionPlanStatus.defaultLineCountStatuses(),
        partId: String? = null,
        lineId: String? = null,
    ) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                repository.getProductionLineCount(
                    planStatusList = planStatusList,
                    partId = partId?.trim()?.takeIf { it.isNotBlank() },
                    lineId = lineId?.trim()?.takeIf { it.isNotBlank() },
                )
            },
            onSuccess = { lineCounts ->
                _productionLineCounts.value = lineCounts
            },
        )
    }

    fun loadProductionStationProgressList(planId: String) {
        val trimmedPlanId = planId.trim()
        if (trimmedPlanId.isBlank()) {
            showToast("生产计划ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.PAGE,
            block = { repository.getProductionStationProgressList(trimmedPlanId) },
            onSuccess = { progressList ->
                _productionStationProgressList.value = progressList
            },
        )
    }

    fun loadProductionBindProgress(planId: String) {
        val trimmedPlanId = planId.trim()
        if (trimmedPlanId.isBlank()) return

        launch(showLoading = false) {
            when (val result = repository.getProductionBindProgress(trimmedPlanId)) {
                is JiangResult.Success -> _productionBindProgress.value = result.data
                is JiangResult.Error -> showToast(result.exception.message)
                JiangResult.Loading -> Unit
                JiangResult.Empty -> Unit
            }
        }
    }

    fun loadProductionSnBindInfo(planId: String, sn: String) {
        val trimmedPlanId = planId.trim()
        val trimmedSn = sn.trim()
        if (trimmedPlanId.isBlank()) {
            showToast("生产计划ID为空")
            return
        }
        if (trimmedSn.isBlank()) {
            showToast("请先扫描PRO码")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "绑定信息加载中...",
            block = { repository.getProductionSnBindInfo(trimmedPlanId, trimmedSn) },
            onSuccess = { info ->
                _productionSnBindInfo.value = info
            },
        )
    }

    fun bindProductionKeyPart(request: ProductionKeyPartBindRequest) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.bindProductionKeyPart(request) },
            onSuccess = { result ->
                _productionKeyPartBindResult.value = result
            },
        )
    }

    fun startProduction(planId: String) {
        val trimmedPlanId = planId.trim()
        if (trimmedPlanId.isBlank()) {
            showToast("生产计划ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "开始生产中...",
            block = { repository.startProduction(trimmedPlanId) },
            onSuccess = {
                _productionStartResult.value = ProductionStartResult(planId = trimmedPlanId)
            },
        )
    }

    fun beginProductionSwitch(planId: String) {
        val trimmedPlanId = planId.trim()
        if (trimmedPlanId.isBlank()) {
            showToast("生产计划ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            block = { repository.beginProductionSwitch(trimmedPlanId) },
            onSuccess = { switchId ->
                _productionSwitchBeginResult.value = ProductionSwitchBeginResult(
                    planId = trimmedPlanId,
                    switchId = switchId,
                )
            },
        )
    }

    fun loadProductionSwitchDetail(planId: String) {
        val trimmedPlanId = planId.trim()
        if (trimmedPlanId.isBlank()) {
            showToast("生产计划ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "生产前核对加载中...",
            block = { repository.getProductionSwitchDetail(trimmedPlanId) },
            onSuccess = { detail ->
                _productionSwitchDetail.value = detail
            },
        )
    }

    fun confirmProductionSoftCheck(lineSwiId: String) {
        val trimmedLineSwiId = lineSwiId.trim()
        if (trimmedLineSwiId.isBlank()) {
            showToast("计划切换ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "软件版本核对提交中...",
            block = { repository.confirmProductionSoftCheck(trimmedLineSwiId) },
            onSuccess = {
                _productionCheckDone.value = Unit
            },
        )
    }

    fun confirmProductionMaterialCheck(lineSwiId: String) {
        val trimmedLineSwiId = lineSwiId.trim()
        if (trimmedLineSwiId.isBlank()) {
            showToast("计划切换ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "物料核对提交中...",
            block = { repository.confirmProductionMaterialCheck(trimmedLineSwiId) },
            onSuccess = {
                _productionCheckDone.value = Unit
            },
        )
    }

    fun confirmProductionPersonCheck(requests: List<ProductionPersonCheckRequest>) {
        if (requests.isEmpty()) {
            showToast("人员资质核对项为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.confirmProductionPersonCheck(requests) },
            onSuccess = {
                _productionCheckDone.value = Unit
            },
        )
    }

    fun confirmProductionEquipmentCheck(requests: List<ProductionEquipmentCheckRequest>) {
        if (requests.isEmpty()) {
            showToast("设备核对项为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.confirmProductionEquipmentCheck(requests) },
            onSuccess = {
                _productionCheckDone.value = Unit
            },
        )
    }

    fun endProductionSwitch(planId: String) {
        val trimmedPlanId = planId.trim()
        if (trimmedPlanId.isBlank()) {
            showToast("生产计划ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "切换完成提交中...",
            block = { repository.endProductionSwitch(trimmedPlanId) },
            onSuccess = {
                _productionSwitchEnded.value = Unit
            },
        )
    }

    fun initUserByAccount(
        account: String,
        status: Int = 1,
        distinct: Boolean = true,
    ) {
        val trimmedAccount = account.trim()
        if (trimmedAccount.isBlank()) {
            showToast("人员账号为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                repository.initUserByAccount(
                    account = trimmedAccount,
                    status = status,
                    distinct = distinct,
                )
            },
            onSuccess = { user ->
                _userByAccount.value = user
            },
        )
    }

    fun loadDebugPlanDetail(taskId: String) {
        val trimmedTaskId = taskId.trim()
        if (trimmedTaskId.isBlank()) {
            showToast("调试计划ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getDebugPlanDetail(trimmedTaskId) },
            onSuccess = { detail ->
                _debugPlanDetail.value = detail
            },
        )
    }

    /**
     * 扫描料箱后的服务端校验，成功后由页面决定是否加入待提交列表。
     */
    fun scanValidDebugBox(iqcTaskId: String, boxQrCode: String) {
        val trimmedTaskId = iqcTaskId.trim()
        val trimmedCode = boxQrCode.trim()
        if (trimmedTaskId.isBlank() || trimmedCode.isBlank()) {
            showToast("箱单校验参数为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.scanValidDebugBox(trimmedTaskId, trimmedCode) },
            onSuccess = { result ->
                _debugBoxScanValidResult.value = result
            },
        )
    }

    /**
     * 创建批次提交前会过滤空料箱码，避免扫码框残留空字符串影响后端校验。
     */
    fun bindDebugBoxes(iqcTaskId: String, boxQrCodeList: List<String>) {
        val trimmedTaskId = iqcTaskId.trim()
        val codes = boxQrCodeList.map { it.trim() }.filter { it.isNotBlank() }
        if (trimmedTaskId.isBlank() || codes.isEmpty()) {
            showToast("箱单绑定参数为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.bindDebugBoxes(trimmedTaskId, codes) },
            onSuccess = {
                _debugOperationDone.value = Unit
            },
        )
    }


    /**
     * 批次执行页扫描 SN 后校验，并取回该 SN 对应的检查项结构。
     */
    fun scanValidDebugSn(taskItemId: String, qrCode: String) {
        val trimmedTaskItemId = taskItemId.trim()
        val trimmedCode = qrCode.trim()
        if (trimmedTaskItemId.isBlank() || trimmedCode.isBlank()) {
            showToast("SN校验参数为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.scanValidDebugSn(trimmedTaskItemId, trimmedCode) },
            onSuccess = { result ->
                _debugSnScanValidResult.value = result
            },
        )
    }

    /**
     * 提交单个 SN 的检查结果，成功后统一通过 debugOperationDone 通知页面清空当前 SN。
     */
    fun submitDebugSnCheck(
        taskItemId: String,
        snRecordId: String,
        ngPhenomena: String,
        checkGroupDto: DebugCheckGroupDto,
        isDraft: Boolean,
    ) {
        val trimmedTaskItemId = taskItemId.trim()
        val trimmedSnRecordId = snRecordId.trim()
        if (trimmedTaskItemId.isBlank() || trimmedSnRecordId.isBlank()) {
            showToast("检查提交参数为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                repository.submitDebugSnCheck(
                    taskItemId = trimmedTaskItemId,
                    snRecordId = trimmedSnRecordId,
                    ngPhenomena = ngPhenomena,
                    checkGroupDto = checkGroupDto,
                    isDraft = isDraft,
                )
            },
            onSuccess = {
                _debugOperationDone.value = Unit
            },
        )
    }

    fun finishProduction(planId: String, forceFinish: Boolean = false) {
        val trimmedPlanId = planId.trim()
        if (trimmedPlanId.isBlank()) {
            showToast("生产计划ID为空")
            return
        }
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "生产完成提交中...",
            block = { repository.finishProduction(trimmedPlanId, forceFinish) },
            onSuccess = { _productionFinishDone.value = Unit },
        )
    }

    fun pauseProduction(planId: String) {
        changeProductionRunState(planId, paused = true)
    }

    fun restartProduction(planId: String) {
        changeProductionRunState(planId, paused = false)
    }

    private fun changeProductionRunState(planId: String, paused: Boolean) {
        val id = planId.trim()
        if (id.isBlank()) {
            showToast("生产计划ID为空")
            return
        }
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = if (paused) "暂停生产中..." else "重新开始生产中...",
            block = {
                if (paused) repository.pauseProduction(id) else repository.restartProduction(id)
            },
            onSuccess = {
                _productionRunStateChanged.value = ProductionRunStateChangeResult(id, paused)
            },
        )
    }

    fun loadDebugPlanProgress(code: String = "") {
        launch(showLoading = false) {
            when (
                val result = repository.getDebugPlanList(
                    DebugPlanListRequest(code = code),
                )
            ) {
                is JiangResult.Success -> _debugPlanProgressPlans.value = result.data
                is JiangResult.Error,
                JiangResult.Loading,
                JiangResult.Empty -> Unit
            }
        }
    }

    fun loadProductionCirculationProgress(planId: String) {
        val id = planId.trim()
        if (id.isBlank()) return
        launchResult(
            loadingMode = JiangLoadingMode.PAGE,
            block = { repository.getProductionCirculationProgress(id) },
            onSuccess = { _productionCirculationProgress.value = it },
        )
    }

    fun loadProductionCirculationInfo(id: String) {
        val circulationId = id.trim()
        if (circulationId.isBlank()) return
        launchResult(
            loadingMode = JiangLoadingMode.PAGE,
            block = { repository.getProductionCirculationInfo(circulationId) },
            onSuccess = { _productionCirculationInfo.value = it },
        )
    }

    fun loadProductionCirculationMaterials(circulationId: String) {
        val id = circulationId.trim()
        if (id.isBlank()) return
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getProductionCirculationMaterials(id) },
            onSuccess = { _productionCirculationMaterials.value = it },
        )
    }

    fun loadProductionCirculationPrintLogs(circulationId: String) {
        val id = circulationId.trim()
        if (id.isBlank()) return
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getProductionCirculationPrintLogs(id) },
            onSuccess = { _productionCirculationPrintLogs.value = it },
        )
    }

    fun printProductionCirculation(request: ProductionCirculationPrintRequest) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "打印中...",
            block = { repository.printProductionCirculation(request) },
            onSuccess = { _productionCirculationPrintDone.value = Unit },
        )
    }

    fun loadProductionOverallTestProgress(planId: String) {
        val trimmedPlanId = planId.trim()
        if (trimmedPlanId.isBlank()) {
            showToast("生产计划ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getProductionOverallTestProgress(trimmedPlanId) },
            onSuccess = { progress ->
                _productionOverallTestProgress.value = progress
            },
        )
    }

    fun loadProductionOverallTestSnInfo(planId: String, proSn: String) {
        val trimmedPlanId = planId.trim()
        val trimmedProSn = proSn.trim()
        if (trimmedPlanId.isBlank()) {
            showToast("生产计划ID为空")
            return
        }
        if (trimmedProSn.isBlank()) {
            showToast("请先扫描产品码")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getProductionOverallTestSnInfo(trimmedPlanId, trimmedProSn) },
            onSuccess = { info ->
                _productionOverallTestSnInfo.value = info
            },
        )
    }

    fun submitProductionOverallTest(
        planId: String,
        proSn: String,
        isDraft: Boolean,
        ngPhenomena: String,
        checkGroupDto: DebugCheckGroupDto,
    ) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                repository.submitProductionOverallTest(
                    planId = planId.trim(),
                    proSn = proSn.trim(),
                    isDraft = isDraft,
                    ngPhenomena = ngPhenomena.trim(),
                    checkGroupDto = checkGroupDto,
                )
            },
            onSuccess = {
                _productionOverallTestOperationDone.value = isDraft
            },
        )
    }

    /**
     * 批次调试提交后的计划级不良数检查使用独立结果，不污染页面通用错误状态。
     */
    fun loadDebugTaskNgLimitPlanDetail(taskId: String) {
        val trimmedTaskId = taskId.trim()
        if (trimmedTaskId.isBlank()) {
            _debugTaskNgLimitPlanResult.value = DebugTaskNgLimitPlanResult(
                taskId = trimmedTaskId,
                message = "调试计划ID为空",
            )
            return
        }
        viewModelScope.launch {
            when (val result = repository.getDebugPlanDetail(trimmedTaskId)) {
                is JiangResult.Success -> {
                    _debugTaskNgLimitPlanResult.value = DebugTaskNgLimitPlanResult(
                        taskId = trimmedTaskId,
                        detail = result.data,
                    )
                }

                is JiangResult.Error -> {
                    _debugTaskNgLimitPlanResult.value = DebugTaskNgLimitPlanResult(
                        taskId = trimmedTaskId,
                        message = result.exception.message.orEmpty(),
                    )
                }

                JiangResult.Empty -> {
                    _debugTaskNgLimitPlanResult.value = DebugTaskNgLimitPlanResult(
                        taskId = trimmedTaskId,
                        message = "调试计划详情为空",
                    )
                }

                JiangResult.Loading -> Unit
            }
        }
    }

    /**
     * 日志是非阻断旁路请求，使用独立结果，避免干扰扫码和 SN 提交的共享页面状态。
     */
    fun saveDebugTaskLog(request: DebugTaskLogRequest) {
        viewModelScope.launch {
            when (val result = repository.saveDebugTaskLog(request)) {
                is JiangResult.Success -> {
                    _debugTaskLogSaveResult.value = DebugTaskLogSaveResult(request, success = true)
                }

                is JiangResult.Error -> {
                    _debugTaskLogSaveResult.value = DebugTaskLogSaveResult(
                        request = request,
                        success = false,
                        message = result.exception.message.orEmpty(),
                    )
                }

                JiangResult.Empty -> {
                    _debugTaskLogSaveResult.value = DebugTaskLogSaveResult(
                        request = request,
                        success = false,
                        message = "日志保存结果为空",
                    )
                }

                JiangResult.Loading -> Unit
            }
        }
    }

    fun scanValidSingleDebugSn(qrCode: String) {
        val trimmedCode = qrCode.trim()
        if (trimmedCode.isBlank()) {
            showToast("请先扫码")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.scanValidSingleDebugSn(trimmedCode) },
            onSuccess = { result ->
                _singleDebugSnScanValidResult.value = result
            },
        )
    }

    fun submitSingleDebugCheck(
        qrCode: String,
        ngPhenomena: String?,
        checkGroupDto: DebugCheckGroupDto,
        isDraft: Boolean,
    ) {
        val trimmedCode = qrCode.trim()
        if (trimmedCode.isBlank()) {
            showToast("请先扫码")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                repository.submitSingleDebugCheck(
                    qrCode = trimmedCode,
                    ngPhenomena = ngPhenomena,
                    checkGroupDto = checkGroupDto,
                    isDraft = isDraft,
                )
            },
            onSuccess = {
                _debugOperationDone.value = Unit
            },
        )
    }

    fun scanValidRecheckSn(qrCode: String) {
        val trimmedCode = qrCode.trim()
        if (trimmedCode.isBlank()) {
            showToast("请先扫码")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.scanValidRecheckSn(trimmedCode) },
            onSuccess = { result ->
                _recheckSnScanValidResult.value = result
            },
        )
    }

    fun submitRecheck(
        qrCode: String,
        ngPhenomena: String?,
        checkGroupDto: DebugCheckGroupDto,
        isDraft: Boolean,
    ) {
        val trimmedCode = qrCode.trim()
        if (trimmedCode.isBlank()) {
            showToast("请先扫码")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                repository.submitRecheck(
                    qrCode = trimmedCode,
                    ngPhenomena = ngPhenomena,
                    checkGroupDto = checkGroupDto,
                    isDraft = isDraft,
                )
            },
            onSuccess = {
                _debugOperationDone.value = Unit
            },
        )
    }

    fun loadDebugTaskItemInfo(id: String) {
        val trimmedId = id.trim()
        if (trimmedId.isBlank()) {
            showToast("批次ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getDebugTaskItemInfo(trimmedId) },
            onSuccess = { result ->
                _debugTaskItemInfo.value = result
            },
        )
    }

    fun finishDebugTaskItem(id: String) {
        val trimmedId = id.trim()
        if (trimmedId.isBlank()) {
            showToast("批次ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.finishDebugTaskItem(trimmedId) },
            onSuccess = {
                _debugOperationDone.value = Unit
            },
        )
    }

    /**
     * 调试计划完工。forcePass 用于后端允许强制放行的场景，默认保持普通完工。
     */
    fun finishDebugPlan(
        id: String,
        fileIdList: List<Long> = emptyList(),
        forcePass: Boolean = false,
    ) {
        val trimmedId = id.trim()
        if (trimmedId.isBlank()) {
            showToast("调试计划ID为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                repository.finishDebugPlan(
                    id = trimmedId,
                    fileIdList = fileIdList,
                    forcePass = forcePass,
                )
            },
            onSuccess = { result ->
                _debugPlanFinishResult.value = result
            },
        )
    }

    fun uploadDebugPlanFinishPhoto(file: File) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "上传拍照凭证中...",
            block = { repository.uploadFile(file) },
            onSuccess = { fileId ->
                _debugPlanPhotoUploadResult.value = fileId
            },
        )
    }

    fun uploadDebugTaskCheckPhoto(checkId: String, file: File) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "上传检查照片中...",
            block = { repository.uploadFile(file) },
            onSuccess = { fileId ->
                _debugTaskCheckPhotoUploadResult.value = DebugTaskCheckPhotoUploadResult(checkId, fileId)
            },
        )
    }

    fun loadDebugDictItems(dictCode: String = DEBUG_DICT_NG_PHENOMENA) {
        val trimmedCode = dictCode.trim()
        if (trimmedCode.isBlank()) {
            showToast("字典编码为空")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getDebugDictItems(trimmedCode) },
            onSuccess = { result ->
                _debugDictItems.value = result
            },
        )
    }

    private companion object {
        const val DEBUG_DICT_NG_PHENOMENA = "ngPhenomena"
    }
}
