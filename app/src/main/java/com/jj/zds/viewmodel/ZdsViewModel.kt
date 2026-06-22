package com.jj.zds.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.common.result.JiangResult
import com.jj.zds.data.dto.debugplan.DebugBoxQrCodeDto
import com.jj.zds.data.dto.common.CurUserDto
import com.jj.zds.data.dto.debugplan.DebugBoxScanValidDto
import com.jj.zds.data.dto.debugplan.DebugCheckGroupDto
import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import com.jj.zds.data.dto.debugplan.DebugPlanDetailDto
import com.jj.zds.data.dto.debugplan.DebugPlanDto
import com.jj.zds.data.dto.debugplan.DebugPlanListRequest
import com.jj.zds.data.dto.debugplan.DebugPlanStatisticsDto
import com.jj.zds.data.dto.debugplan.DebugSnScanValidDto
import com.jj.zds.data.dto.debugplan.DebugTaskItemInfoDto
import com.jj.zds.data.dto.login.LoginRequest
import com.jj.zds.data.dto.menu.ZdsMenuDto
import com.jj.zds.data.dto.productionplan.LineDto
import com.jj.zds.data.dto.productionplan.ProductionEquipmentCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionPlanDto
import com.jj.zds.data.dto.productionplan.ProductionPlanStatus
import com.jj.zds.data.dto.productionplan.ProductionPersonCheckRequest
import com.jj.zds.data.dto.productionplan.ProductionSwitchDetailDto
import com.jj.zds.data.dto.qrcode.QrCodeDto
import com.jj.zds.data.dto.repair.RepairAction
import com.jj.zds.data.dto.repair.RepairLogDto
import com.jj.zds.data.dto.repair.RepairSnScanValidDto
import com.jj.zds.data.dto.repair.RepairSubmitRequest
import com.jj.zds.data.dto.update.ZdsVersionCheckResult
import com.jj.zds.data.repository.ZdsRepository
import java.io.File

data class DebugTaskCheckPhotoUploadResult(
    val checkId: String,
    val fileId: Long,
)

data class ProductionSwitchBeginResult(
    val planId: String,
    val switchId: Int,
)

class ZdsViewModel : JiangViewModel() {

    private val repository by lazy { ZdsRepository() }

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

    private val _repairPhotoUploadResult = MutableLiveData<Long>()
    val repairPhotoUploadResult: LiveData<Long> = _repairPhotoUploadResult

    private val _debugPlanStatistics = MutableLiveData<DebugPlanStatisticsDto>()
    val debugPlanStatistics: LiveData<DebugPlanStatisticsDto> = _debugPlanStatistics

    private val _debugPlans = MutableLiveData<List<DebugPlanDto>>()
    val debugPlans: LiveData<List<DebugPlanDto>> = _debugPlans

    private val _productionPlans = MutableLiveData<List<ProductionPlanDto>>()
    val productionPlans: LiveData<List<ProductionPlanDto>> = _productionPlans

    private val _productionLineCounts = MutableLiveData<List<LineDto>>()
    val productionLineCounts: LiveData<List<LineDto>> = _productionLineCounts

    private val _productionSwitchBeginResult = MutableLiveData<ProductionSwitchBeginResult>()
    val productionSwitchBeginResult: LiveData<ProductionSwitchBeginResult> = _productionSwitchBeginResult

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
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                repository.getDebugPlanList(
                    DebugPlanListRequest(
                        categoryName = categoryName,
                        code = code,
                        status = status,
                    ),
                )
            },
            onSuccess = { plans ->
                _debugPlans.value = plans
            },
        )
    }

    fun loadProductionPlans(
        keyword: String = "",
        lineId: String? = null,
    ) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                repository.getProductionPlanList(
                    keyword = keyword.trim(),
                    lineId = lineId,
                )
            },
            onSuccess = { plans ->
                _productionPlans.value = plans
            },
        )
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
     * 创建批次页面扫描料箱码后先解析，拿到料箱详情后再允许加入待提交队列。
     */
    fun analysisBoxQrCode(qrCode: String) {
        val trimmedCode = qrCode.trim()
        if (trimmedCode.isBlank()) {
            showToast("请先扫描料箱码")
            return
        }

        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.analysisBoxQrCode(trimmedCode) },
            onSuccess = { result ->
                _debugBoxQrCodeResult.value = result
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
                )
            },
            onSuccess = {
                _debugOperationDone.value = Unit
            },
        )
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
