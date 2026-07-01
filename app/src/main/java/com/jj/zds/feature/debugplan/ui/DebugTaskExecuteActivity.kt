package com.jj.zds.feature.debugplan.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.text.InputFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.jj.core.permission.hasJiangPermission
import com.jj.storage.JiangStorage
import androidx.recyclerview.widget.ConcatAdapter
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.dialog.JInputDialog
import com.jj.ui.dialog.JSingleChoiceDialog
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.recycler.multitype.JMultiBindingAdapter
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.ui.toolbar.JiangToolbarMenuItem
import com.jj.zds.data.dto.debugplan.DebugCheckGroupDto
import com.jj.zds.data.dto.debugplan.DebugCheckItemDto
import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import com.jj.zds.data.dto.debugplan.DebugSnScanValidDto
import com.jj.zds.data.dto.debugplan.DebugTaskItemInfoDto
import com.jj.zds.data.dto.debugplan.DebugTaskLogRequest
import com.jj.zds.data.local.ZdsStorageKeys
import com.jj.zds.databinding.ActivityDebugTaskExecuteBinding
import com.jj.zds.databinding.ItemDebugTaskExecuteFooterBinding
import com.jj.zds.databinding.ItemDebugTaskExecuteHeaderBinding
import com.jj.zds.feature.debugplan.adapter.DebugTaskPhotoBinder
import com.jj.zds.feature.debugplan.adapter.DebugTaskSwitchBinder
import com.jj.zds.feature.debugplan.adapter.DebugTaskTextBinder
import com.jj.zds.feature.debugplan.adapter.DebugTaskValueBinder
import com.jj.zds.feature.debugplan.data.DebugTaskCheckResult
import com.jj.zds.feature.debugplan.data.DebugTaskCheckRow
import com.jj.zds.feature.debugplan.data.DebugTaskPhotoItem
import com.jj.zds.feature.debugplan.data.DebugTaskPhotoRow
import com.jj.zds.feature.debugplan.data.DebugTaskSwitchRow
import com.jj.zds.feature.debugplan.data.DebugTaskTextRow
import com.jj.zds.feature.debugplan.data.DebugTaskValueRow
import com.jj.zds.feature.photo.PhotoUploadFileProcessor
import com.jj.zds.feature.photo.PhotoUploadMode
import com.jj.zds.feature.repair.data.buildRepairFileUrl
import com.jj.zds.viewmodel.ZdsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class DebugTaskExecuteActivity : JiangToolbarStateVmActivity<ActivityDebugTaskExecuteBinding, ZdsViewModel>() {

    private var taskItemId: String = ""
    private var snCode: String = ""
    private var lastSubmitCode: String = ""
    private var lastSubmitAt: Long = 0L
    private var scanInProgress = false
    private var activeOperation = ActiveOperation.NONE
    private var taskInfo: DebugTaskItemInfoDto? = null
    private var currentPlanId: String = ""
    private var currentSn: DebugSnScanValidDto? = null
    private var currentCheckGroup: DebugCheckGroupDto? = null
    private var pendingPhotoCheckId: String? = null
    private var pendingPhotoFile: File? = null
    private var clearingSnForm = false
    private var pendingNgLimitCheck = false
    private var ngLimitPromptHandled = false
    private var pendingLogRequest: DebugTaskLogRequest? = null
    private var logSaveInProgress = false
    private var logRetryAttempted = false
    private val checks = mutableListOf<DebugTaskCheckRow>()

    private val headerAdapter = JBindingAdapter(
        inflate = ItemDebugTaskExecuteHeaderBinding::inflate,
        onBind = ::bindHeader,
    )
    private val checkAdapter = JMultiBindingAdapter<DebugTaskCheckRow>()
        .addItemBinder(DebugTaskSwitchBinder(::updateSwitchResult))
        .addItemBinder(DebugTaskValueBinder(::updateValueResult, ::scrollCheckIntoView))
        .addItemBinder(DebugTaskTextBinder(::showRemarkDialog))
        .addItemBinder(
            DebugTaskPhotoBinder(
                onPhotoClick = ::requestCheckPhoto,
                onUploadModeChange = ::updatePhotoUploadMode,
                onPreviewClick = ::previewCheckPhoto,
                onDeleteClick = ::deleteCheckPhoto,
                tokenProvider = ::currentToken,
            ),
        )
    private val footerAdapter = JBindingAdapter<Unit, ItemDebugTaskExecuteFooterBinding>(
        inflate = ItemDebugTaskExecuteFooterBinding::inflate,
        onBind = ::bindFooter,
    )

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchCheckPhotoCamera()
        } else {
            JToast.show("未授予相机权限，无法拍照")
        }
    }

    private val takeCheckPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val checkId = pendingPhotoCheckId
        val file = pendingPhotoFile
        if (success && !checkId.isNullOrBlank() && file != null) {
            val row = checks.firstOrNull { it.checkId == checkId } as? DebugTaskPhotoRow
            val mode = row?.uploadMode ?: PhotoUploadMode.default()
            updatePhotoUploading(checkId, uploading = true)
            val uploadFile = PhotoUploadFileProcessor.prepareUploadFile(
                source = file,
                mode = mode,
                cacheDir = cacheDir,
                prefix = "debug_task_check",
            )
            viewModel.uploadDebugTaskCheckPhoto(checkId, uploadFile)
        } else {
            pendingPhotoCheckId = null
            pendingPhotoFile = null
            JToast.show("未完成拍照")
        }
    }

    override fun createViewBinding(inflater: LayoutInflater): ActivityDebugTaskExecuteBinding {
        return ActivityDebugTaskExecuteBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        taskItemId = intent.getStringExtra(EXTRA_TASK_ITEM_ID).orEmpty()
        if (taskItemId.isBlank()) {
            JToast.show("批次ID为空")
            finish()
            return
        }

        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        bindScanInput()
        checkAdapter.setDiffCallback(
            areItemsTheSame = { oldItem, newItem -> oldItem.rowId == newItem.rowId },
            areContentsTheSame = { oldItem, newItem -> oldItem == newItem },
        )
        binding.recyclerChecks.setupLinear(
            ConcatAdapter(
                headerAdapter,
                checkAdapter,
                footerAdapter,
            ),
        )
        binding.recyclerChecks.preserveFocusAfterLayout = false
        binding.recyclerChecks.addDefaultDivider()
        bindImeInsets()
        headerAdapter.setList(listOf(Unit))
        footerAdapter.setList(listOf(Unit))
        renderChecks()
        refreshTaskInfo()
        binding.inputSn.post {
            binding.inputSn.requestInputFocus(showKeyboard = false)
        }
    }

    override fun initPageObserver() {
        viewModel.debugTaskItemInfo.observe(this) { info ->
            taskInfo = info
            currentPlanId = info.taskId.orEmpty()
            toolbar.setSubtitleText(info.toBatchInfoText())
            headerAdapter.updateItem(0, Unit)
            footerAdapter.updateItem(0, Unit)
            if (pendingNgLimitCheck) {
                if (currentPlanId.isBlank()) {
                    pendingNgLimitCheck = false
                    JToast.show("调试计划ID为空，暂时无法检查不良数上限")
                } else {
                    viewModel.loadDebugTaskNgLimitPlanDetail(currentPlanId)
                }
            }
        }
        viewModel.debugTaskNgLimitPlanResult.observe(this) { result ->
            if (!pendingNgLimitCheck || result.taskId != currentPlanId) return@observe
            pendingNgLimitCheck = false
            val detail = result.detail
            if (detail == null) {
                JToast.show(result.message.ifBlank { "暂时无法检查不良数上限" })
                return@observe
            }
            if (
                DebugTaskNgLimitRules.shouldWarn(
                    ngCount = detail.ngCount,
                    ngLimit = detail.rejectionQty,
                    sessionAcknowledged = ngLimitPromptHandled,
                )
            ) {
                showNgLimitDialog(detail.ngCount, detail.rejectionQty)
            }
        }
        viewModel.debugSnScanValidResult.observe(this) { result ->
            scanInProgress = false
            renderSnScanResult(result)
        }
        viewModel.debugDictItems.observe(this) { items ->
            if (activeOperation == ActiveOperation.WAITING_NG) {
                showNgPhenomenaDialog(items)
            }
        }
        viewModel.debugTaskCheckPhotoUploadResult.observe(this) { result ->
            handleCheckPhotoUploaded(result.checkId, result.fileId)
        }
        viewModel.debugTaskLogSaveResult.observe(this) { result ->
            if (result.request != pendingLogRequest) return@observe
            logSaveInProgress = false
            if (result.success) {
                pendingLogRequest = null
            } else {
                JToast.show(result.message.ifBlank { "调试日志保存失败，稍后自动重试" })
            }
        }
        viewModel.debugOperationDone.observe(this) {
            when (activeOperation) {
                ActiveOperation.SAVE_DRAFT -> handleDraftSaved()
                ActiveOperation.SUBMIT_SN -> handleSnSubmitDone()
                ActiveOperation.FINISH_BATCH -> handleBatchFinishDone()
                else -> Unit
            }
        }
    }

    override fun toolbarTitle(): CharSequence = "开始调试"

    override fun toolbarSubtitle(): CharSequence = "SN 调试"

    override fun toolbarMenuItems(): List<JiangToolbarMenuItem> {
        return listOf(
            JiangToolbarMenuItem(MENU_SYNC, "刷新"),
            JiangToolbarMenuItem(MENU_FINISH_BATCH, "批次完成"),
        )
    }

    override fun onToolbarMenuClick(item: JiangToolbarMenuItem) {
        when (item.id) {
            MENU_SYNC -> refreshTaskInfo()
            MENU_FINISH_BATCH -> confirmFinishBatch()
        }
    }

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        val wasScanInProgress = scanInProgress
        val operation = activeOperation
        scanInProgress = false
        if (wasScanInProgress) {
            showScanError(message)
            return
        }
        if (operation == ActiveOperation.FINISH_BATCH) {
            activeOperation = ActiveOperation.NONE
            footerAdapter.updateItem(0, Unit)
            showBatchFinishError(message)
            return
        }
        pendingPhotoCheckId?.let { checkId ->
            updatePhotoUploading(checkId, uploading = false)
            pendingPhotoCheckId = null
            pendingPhotoFile = null
        }
        activeOperation = ActiveOperation.NONE
        footerAdapter.updateItem(0, Unit)
        focusScanInput(selectAll = true)
        super.onUiError(code, message, throwable)
    }

    private fun bindScanInput() {
        binding.inputSn.setText(snCode)
        binding.inputSn.onTextChanged {
            snCode = it
            if (it != lastSubmitCode) {
                lastSubmitCode = ""
            }
            binding.inputSn.clearState()
            if (!clearingSnForm && it.isBlank() && currentSn != null) {
                resetSnForm()
            }
        }
        binding.inputSn.onInputDone {
            submitScanSn(it)
        }
        binding.inputSn.onTextDebounce(delayMillis = 260L) {
            submitScanSn(it)
        }
    }

    private fun bindHeader(
        itemBinding: ItemDebugTaskExecuteHeaderBinding,
        item: Unit,
        position: Int,
    ) {
        itemBinding.tvFormName.text = currentCheckGroup?.let {
            "${it.name.orDash()} · V${it.version}"
        } ?: "等待扫描 SN"
    }

    private fun bindFooter(
        itemBinding: ItemDebugTaskExecuteFooterBinding,
        item: Unit,
        position: Int,
    ) {
        val pendingCount = checks.count { it.result == DebugTaskCheckResult.PENDING && !it.dto.isSkipable }
        val failCount = DebugTaskSubmitRules.reportableFailureCount(checks)
        itemBinding.tvSubmitHint.text = when {
            currentSn == null -> "请先扫描 SN"
            pendingCount > 0 -> "还有 $pendingCount 个检查项未完成"
            failCount > 0 -> "已完成，发现 $failCount 个不良"
            else -> "检查项已全部完成"
        }
        val isSaving = activeOperation == ActiveOperation.SAVE_DRAFT
        val isSubmitting = activeOperation == ActiveOperation.SUBMIT_SN
        val isBusy = isSaving || isSubmitting
        itemBinding.btnSave.text = if (isSaving) "保存中..." else "保存"
        itemBinding.btnSave.isEnabled = currentSn != null && !isBusy
        itemBinding.btnSave.alpha = if (itemBinding.btnSave.isEnabled) 1f else 0.6f
        itemBinding.btnSave.setOnClickListener {
            saveDraft()
        }
        itemBinding.btnSubmit.text = if (isSubmitting) "提交中..." else "提交"
        itemBinding.btnSubmit.isEnabled = currentSn != null && !isBusy
        itemBinding.btnSubmit.alpha = if (itemBinding.btnSubmit.isEnabled) 1f else 0.6f
        itemBinding.btnSubmit.setOnClickListener {
            submitChecks()
        }
    }

    /**
     * 扫码枪和软键盘都会触发这里；用 scanInProgress + 时间窗避免同一 SN 被连续提交。
     */
    private fun submitScanSn(rawCode: String) {
        val code = rawCode.trim()
        val now = SystemClock.elapsedRealtime()
        if (code.isBlank()) return
        if (scanInProgress) return
        if (code == lastSubmitCode && now - lastSubmitAt < SUBMIT_DEBOUNCE_WINDOW) return
        lastSubmitCode = code
        lastSubmitAt = now
        scanInProgress = true
        binding.inputSn.verifying("SN 校验中...")
        viewModel.scanValidDebugSn(taskItemId, code)
    }

    /**
     * SN 校验成功后重建当前点检表单：SN 信息、检查组和检查项列表必须保持同一份数据。
     */
    private fun renderSnScanResult(result: DebugSnScanValidDto) {
        currentSn = result
        snCode = result.sn ?: result.qrCode ?: snCode
        currentCheckGroup = result.checkGrp ?: result.checkGroupDto
        checks.clear()
        checks += currentCheckGroup?.toRows().orEmpty()
        headerAdapter.updateItem(0, Unit)
        binding.inputSn.showSuccess("SN 校验通过")
        renderChecks()
        footerAdapter.updateItem(0, Unit)
        if (checks.isEmpty()) {
            JToast.show("该 SN 未返回检查项")
        }
    }

    private fun renderChecks() {
        checkAdapter.submitListWithDiff(checks.toList())
    }

    private fun updateSwitchResult(id: String, result: DebugTaskCheckResult) {
        updateCheck(id) { item ->
            (item as? DebugTaskSwitchRow)?.copy(result = result) ?: item
        }
    }

    private fun updateValueResult(id: String, value: String, result: DebugTaskCheckResult) {
        updateCheck(id, notifyItem = false) { item ->
            (item as? DebugTaskValueRow)?.copy(inputValue = value, result = result) ?: item
        }
    }

    private fun showRemarkDialog(id: String) {
        val item = checks.firstOrNull { it.checkId == id } as? DebugTaskTextRow ?: return
        object : JInputDialog(this, this) {
            init {
                (inputLayout.focusedInputView() as? EditText)?.filters = arrayOf(
                    InputFilter.LengthFilter(DebugTaskInputRules.MAX_INPUT_LENGTH),
                )
            }
        }
            .setTitle(item.title)
            .setInputTitle("调试说明")
            .setHint(item.expectedText.ifBlank { "输入提示文本内容" })
            .setText(item.remark)
            .setOnConfirm { remark ->
                updateCheck(id) { oldItem ->
                    (oldItem as? DebugTaskTextRow)?.copy(
                        remark = remark,
                        result = remark.toTextResult(oldItem.expectedText),
                    ) ?: oldItem
                }
            }
            .selectAll()
            .show()
    }

    private fun requestCheckPhoto(id: String) {
        val item = checks.firstOrNull { it.checkId == id } as? DebugTaskPhotoRow ?: return
        if (item.photos.size >= MAX_PHOTO_COUNT) {
            JToast.show("检查照片最多9张")
            return
        }
        pendingPhotoCheckId = id
        if (hasJiangPermission(Manifest.permission.CAMERA)) {
            launchCheckPhotoCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun updatePhotoUploadMode(id: String, mode: PhotoUploadMode) {
        updateCheck(id) { item ->
            (item as? DebugTaskPhotoRow)?.copy(uploadMode = mode) ?: item
        }
    }

    private fun updatePhotoUploading(id: String, uploading: Boolean) {
        updateCheck(id) { item ->
            (item as? DebugTaskPhotoRow)?.copy(uploading = uploading) ?: item
        }
    }

    private fun launchCheckPhotoCamera() {
        val checkId = pendingPhotoCheckId
        if (checkId.isNullOrBlank()) return
        val file = createCheckPhotoFile(checkId)
        pendingPhotoFile = file
        takeCheckPhotoLauncher.launch(file.toProviderUri())
    }

    private fun handleCheckPhotoUploaded(checkId: String, fileId: Long) {
        val localFile = pendingPhotoFile
        pendingPhotoCheckId = null
        pendingPhotoFile = null
        updateCheck(checkId) { item ->
            (item as? DebugTaskPhotoRow)?.let {
                val photos = (it.photos + DebugTaskPhotoItem(fileId = fileId.toString(), localFile = localFile))
                    .take(MAX_PHOTO_COUNT)
                it.copy(
                    photos = photos,
                    uploading = false,
                    result = if (photos.isEmpty()) DebugTaskCheckResult.PENDING else DebugTaskCheckResult.PASS,
                )
            } ?: item
        }
       // JToast.show("检查照片已上传")
    }

    private fun deleteCheckPhoto(id: String, index: Int) {
        updateCheck(id) { item ->
            (item as? DebugTaskPhotoRow)?.let {
                val photo = it.photos.getOrNull(index) ?: return@let it
                photo.localFile?.delete()
                val photos = it.photos.toMutableList().apply { removeAt(index) }
                it.copy(
                    photos = photos,
                    result = if (photos.isEmpty()) DebugTaskCheckResult.PENDING else DebugTaskCheckResult.PASS,
                )
            } ?: item
        }
    }

    private fun previewCheckPhoto(id: String, index: Int) {
        val row = checks.firstOrNull { it.checkId == id } as? DebugTaskPhotoRow ?: return
        val photo = row.photos.getOrNull(index) ?: return
        showPhotoPreview(photo)
    }

    /**
     * 更新单条检查项。文本/开关类刷新整行，数值输入只换数据，避免输入框焦点被 RecyclerView 抢走。
     */
    private fun updateCheck(
        id: String,
        notifyItem: Boolean = true,
        transform: (DebugTaskCheckRow) -> DebugTaskCheckRow,
    ) {
        val position = checks.indexOfFirst { it.checkId == id }
        if (position < 0) return
        val updated = transform(checks[position])
        checks[position] = updated
        if (notifyItem) {
            checkAdapter.updateItem(position, updated, PAYLOAD_CHECK_CHANGED)
        } else {
            checkAdapter.updateItemData(position, updated)
        }
        footerAdapter.updateItem(0, Unit)
    }

    /**
     * 提交前先判断 NG：系统按顺序记录第一条 NG，所以出现 NG 后不再拦截后续未完成项。
     */
    private fun submitChecks() {
        val sn = currentSn
        val group = currentCheckGroup
        if (sn == null || group == null) {
            JToast.show("请先扫描 SN")
            focusScanInput(selectAll = false)
            return
        }
        if (DebugTaskSubmitRules.hasReportableFailure(checks)) {
            activeOperation = ActiveOperation.WAITING_NG
            viewModel.loadDebugDictItems()
            return
        }
        val pendingCount = checks.count { it.result == DebugTaskCheckResult.PENDING && !it.dto.isSkipable }
        if (pendingCount > 0) {
            JToast.show("还有 $pendingCount 个检查项未完成")
            scrollToFirstPending()
            return
        }
        submitSnCheck(ngPhenomena = "", mode = DebugTaskSubmitMode.SUBMIT)
    }

    private fun saveDraft() {
        if (currentSn == null || currentCheckGroup == null) {
            JToast.show("请先扫描 SN")
            focusScanInput(selectAll = false)
            return
        }
        submitSnCheck(ngPhenomena = "", mode = DebugTaskSubmitMode.DRAFT)
    }

    private fun showNgPhenomenaDialog(items: List<DebugDictItemDto>) {
        if (items.isEmpty()) {
            JToast.show("不良现象列表为空")
            activeOperation = ActiveOperation.NONE
            return
        }
        activeOperation = ActiveOperation.NONE
        JSingleChoiceDialog<DebugDictItemDto>(this, this)
            .setTitle("选择不良现象")
            .setItems(items) { it.label ?: it.value ?: it.id.orDash() }
            .setOnConfirm { selected ->
                val value = selected?.value ?: selected?.label
                if (value.isNullOrBlank()) {
                    JToast.show("请选择不良现象")
                    activeOperation = ActiveOperation.NONE
                } else {
                    submitSnCheck(value, mode = DebugTaskSubmitMode.SUBMIT)
                }
            }
            .show()
    }

    /**
     * 真正提交 SN 点检结果前，把页面行数据回填到后端原始 checkGroupDto 结构中。
     */
    private fun submitSnCheck(ngPhenomena: String, mode: DebugTaskSubmitMode) {
        val sn = currentSn ?: return
        val group = currentCheckGroup ?: return
        val snRecordId = sn.id.orEmpty()
        if (snRecordId.isBlank()) {
            JToast.show("SN记录ID为空")
            activeOperation = ActiveOperation.NONE
            return
        }
        activeOperation = if (mode == DebugTaskSubmitMode.DRAFT) {
            ActiveOperation.SAVE_DRAFT
        } else {
            ActiveOperation.SUBMIT_SN
        }
        footerAdapter.updateItem(0, Unit)
        viewModel.submitDebugSnCheck(
            taskItemId = taskItemId,
            snRecordId = snRecordId,
            ngPhenomena = ngPhenomena,
            checkGroupDto = group.withRowResults(checks).copy(status = mode.status),
            isDraft = mode == DebugTaskSubmitMode.DRAFT,
        )
    }

    private fun handleDraftSaved() {
        activeOperation = ActiveOperation.NONE
        footerAdapter.updateItem(0, Unit)
        JToast.show("草稿已保存")
    }

    /**
     * 单个 SN 提交成功后清空当前表单并刷新批次信息，下一次扫码从干净状态开始。
     */
    private fun handleSnSubmitDone() {
        pendingNgLimitCheck = true
        retryPendingLogOnce()
        resetSnForm(clearInput = true)
        refreshTaskInfo()
        focusScanInput(selectAll = false)
        JToast.show("SN调试已提交")
    }

    private fun showNgLimitDialog(ngCount: Int, ngLimit: Int) {
        JConfirmDialog(this, this)
            .setTitle("批次不良数已超限")
            .setMessage("当前调试计划不良数为 $ngCount，已超过计划上限 $ngLimit，请选择后续处理方式。")
            .setCancelText("联系主管")
            .setConfirmText("继续工作")
            .setCancelable(false)
            .setOnCancel {
                handleNgLimitAction(DebugTaskNgLimitAction.CONTACT_SUPERVISOR, ngCount, ngLimit)
            }
            .setOnConfirm {
                handleNgLimitAction(DebugTaskNgLimitAction.CONTINUE_WORK, ngCount, ngLimit)
            }
            .show()
    }

    private fun handleNgLimitAction(
        action: DebugTaskNgLimitAction,
        ngCount: Int,
        ngLimit: Int,
    ) {
        ngLimitPromptHandled = true
        val log = DebugTaskNgLimitRules.buildLog(action, ngCount, ngLimit)
        val request = DebugTaskLogRequest(
            taskId = currentPlanId,
            taskItemId = taskItemId,
            opName = log.opName,
            opDesc = log.opDesc,
        )
        pendingLogRequest = request
        logRetryAttempted = false
        submitPendingLog(request)
    }

    private fun submitPendingLog(request: DebugTaskLogRequest) {
        logSaveInProgress = true
        viewModel.saveDebugTaskLog(request)
    }

    private fun retryPendingLogOnce() {
        val request = pendingLogRequest ?: return
        if (logSaveInProgress || logRetryAttempted) return
        logRetryAttempted = true
        submitPendingLog(request)
    }

    private fun resetSnForm(clearInput: Boolean = false) {
        clearingSnForm = clearInput
        if (clearInput) {
            binding.inputSn.setText("")
        }
        clearingSnForm = false
        activeOperation = ActiveOperation.NONE
        currentSn = null
        currentCheckGroup = null
        pendingPhotoCheckId = null
        pendingPhotoFile = null
        checks.clear()
        snCode = ""
        lastSubmitCode = ""
        lastSubmitAt = 0L
        scanInProgress = false
        binding.inputSn.clearState()
        renderChecks()
        headerAdapter.updateItem(0, Unit)
        footerAdapter.updateItem(0, Unit)
    }

    private fun confirmFinishBatch() {
        JConfirmDialog(this, this)
            .setTitle("批次完工")
            .setMessage("确认当前批次已经全部调试完成？")
            .setConfirmText("确认完工")
            .setOnConfirm {
                activeOperation = ActiveOperation.FINISH_BATCH
                footerAdapter.updateItem(0, Unit)
                viewModel.finishDebugTaskItem(taskItemId)
            }
            .show()
    }

    private fun handleBatchFinishDone() {
        activeOperation = ActiveOperation.NONE
        footerAdapter.updateItem(0, Unit)
        finish()
        JToast.show("批次已完工")
    }

    private fun refreshTaskInfo() {
        viewModel.loadDebugTaskItemInfo(taskItemId)
    }

    private fun createCheckPhotoFile(checkId: String): File {
        val dir = File(cacheDir, "debug_task_check_photo").apply {
            mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        return File(dir, "debug_check_${checkId}_$timestamp.jpg")
    }

    private fun File.toProviderUri(): Uri {
        return FileProvider.getUriForFile(
            this@DebugTaskExecuteActivity,
            "${packageName}.jiang.fileprovider",
            this,
        )
    }

    private fun showPhotoPreview(photo: DebugTaskPhotoItem) {
        val popupRoot = FrameLayout(this).apply {
            setBackgroundColor(0xF0000000.toInt())
            setPadding(dp(12), dp(32), dp(12), dp(24))
        }
        val imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            )
            adjustViewBounds = false
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { }
        }
        val closeView = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END)
            gravity = Gravity.CENTER
            text = "×"
            textSize = 30f
            setTextColor(Color.WHITE)
            contentDescription = "关闭图片预览"
        }
        popupRoot.addView(imageView)
        popupRoot.addView(closeView)
        val popupWindow = PopupWindow(
            popupRoot,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true,
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        popupRoot.setOnClickListener { popupWindow.dismiss() }
        closeView.setOnClickListener { popupWindow.dismiss() }
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
        popupRoot.post {
            photo.localFile?.takeIf { it.exists() }?.let {
                imageView.load(it)
            } ?: buildRepairFileUrl(photo.fileId, currentToken())?.let {
                imageView.load(it)
            }
        }
    }

    private fun currentToken(): String {
        return JiangStorage.kv.getString(ZdsStorageKeys.MES_UP_TOKEN, "").orEmpty()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun bindImeInsets() {
        val defaultBottomPadding = binding.recyclerChecks.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            binding.recyclerChecks.setPadding(
                binding.recyclerChecks.paddingLeft,
                binding.recyclerChecks.paddingTop,
                binding.recyclerChecks.paddingRight,
                defaultBottomPadding + if (imeVisible) imeBottom else 0,
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    /**
     * 数值输入框获得焦点时把整行滚到可见区域，减少软键盘遮挡。
     */
    private fun scrollCheckIntoView(id: String) {
        val position = checks.indexOfFirst { it.checkId == id }
        if (position < 0) return
        val adapterPosition = position + HEADER_ITEM_COUNT
        binding.recyclerChecks.post { requestCheckVisible(adapterPosition) }
        binding.recyclerChecks.postDelayed({ requestCheckVisible(adapterPosition) }, 180L)
    }

    private fun scrollToFirstPending() {
        val position = checks.indexOfFirst { it.result == DebugTaskCheckResult.PENDING && !it.dto.isSkipable }
        if (position < 0) return
        binding.recyclerChecks.smoothScrollToPosition(position + HEADER_ITEM_COUNT)
    }

    private fun requestCheckVisible(position: Int) {
        binding.recyclerChecks.findViewHolderForAdapterPosition(position)?.itemView?.let { itemView ->
            itemView.requestRectangleOnScreen(
                Rect(0, 0, itemView.width, itemView.height),
                false,
            )
        }
    }

    private fun showScanError(message: String) {
        binding.inputSn.showError(message).selectAll()
        JConfirmDialog(this, this)
            .setTitle("操作失败")
            .setMessage(message)
            .setConfirmText("知道了")
            .setShowCancelButton(false)
            .setOnConfirm {
                focusScanInput(selectAll = true)
            }
            .setOnDismiss {
                focusScanInput(selectAll = true)
            }
            .show()
    }

    private fun showBatchFinishError(message: String) {
        JConfirmDialog(this, this)
            .setTitle("批次完工失败")
            .setMessage(message)
            .setConfirmText("知道了")
            .setShowCancelButton(false)
            .show()
    }

    private fun focusScanInput(selectAll: Boolean) {
        binding.inputSn.post {
            binding.inputSn.requestInputFocus(showKeyboard = false)
            if (selectAll) {
                binding.inputSn.selectAll()
            }
        }
    }

    private fun DebugTaskItemInfoDto.toBatchInfoText(): String {
        return buildString {
            append("批次 ").append(batchNo.orDash())
            append(" / OK ").append(okCount)
            append(" / NG ").append(ngCount)
            if (boxLabelList.isNotEmpty()) {
                append(" / 箱 ").append(boxLabelList.size)
            }
        }
    }

    /**
     * 后端返回的是三级检查结构，这里拍平成 RecyclerView 行，同时保留排序和层级编号。
     */
    private fun DebugCheckGroupDto.toRows(): List<DebugTaskCheckRow> {
        return checkList
            .sortedBy { it.sort }
            .flatMapIndexed { firstIndex, first ->
                first.children.sortedBy { it.sort }.flatMapIndexed { secondIndex, second ->
                    second.children.sortedBy { it.sort }.mapIndexed { itemIndex, item ->
                        item.toRow(
                            typeLabel = "${firstIndex + 1}.${secondIndex + 1}.${itemIndex + 1}",
                            parentKey = "${first.name.orEmpty()}-${second.id.orEmpty()}-${itemIndex}",
                        )
                    }
                }
            }
    }

    /**
     * 根据采集类型把后端检查项转换成页面行：数值、文本、默认开关三类。
     */
    private fun DebugCheckItemDto.toRow(
        typeLabel: String,
        parentKey: String,
    ): DebugTaskCheckRow {
        val checkId = id ?: itemId ?: "$parentKey-${name.orEmpty()}"
        val title = name.orDash()
        val itemDescription = description.orEmpty()
        val expectedText = standardOption?.takeIf { it.isNotBlank() } ?: itemDescription
        val rowId = "$parentKey-$checkId"
        val initialText = result?.toString().orEmpty()
        return when (DebugTaskInputRules.resolveType(collectType, minValue, maxValue)) {
            DebugTaskCollectType.NUM -> DebugTaskValueRow(
                rowId = rowId,
                checkId = checkId,
                typeLabel = typeLabel,
                title = title,
                description = itemDescription,
                dto = this,
                inputValue = initialText.toDraftNumberText(),
                referenceValue = buildReferenceText(),
                result = initialText.toInitialValueResult(minValue, maxValue),
            )
            DebugTaskCollectType.INPUT -> DebugTaskTextRow(
                rowId = rowId,
                checkId = checkId,
                typeLabel = typeLabel,
                title = title,
                description = itemDescription,
                dto = this,
                remark = initialText,
                expectedText = expectedText,
                result = initialText.toTextResult(expectedText),
            )
            DebugTaskCollectType.PHOTO -> DebugTaskPhotoRow(
                rowId = rowId,
                checkId = checkId,
                typeLabel = typeLabel,
                title = title,
                description = itemDescription,
                dto = this,
                photos = initialText.toPhotoItems(),
                result = if (initialText.toPhotoItems().isNotEmpty()) {
                    DebugTaskCheckResult.PASS
                } else {
                    DebugTaskCheckResult.PENDING
                },
            )
            DebugTaskCollectType.CONFIRM -> DebugTaskSwitchRow(
                rowId = rowId,
                checkId = checkId,
                typeLabel = typeLabel,
                title = title,
                description = itemDescription,
                dto = this,
                result = result.toInitialSwitchResult(),
            )
        }
    }

    /**
     * 提交时按展示顺序把行结果写回原始三级结构，保持后端字段和层级不变。
     */
    private fun DebugCheckGroupDto.withRowResults(rows: List<DebugTaskCheckRow>): DebugCheckGroupDto {
        val rowIterator = rows.iterator()
        return copy(
            checkList = checkList.map { first ->
                first.copy(
                    children = first.children.map { second ->
                        second.copy(
                            children = second.children.map { item ->
                                item.withRowResult(if (rowIterator.hasNext()) rowIterator.next() else null)
                            },
                        )
                    },
                )
            },
        )
    }

    private fun DebugCheckItemDto.withRowResult(row: DebugTaskCheckRow?): DebugCheckItemDto {
        return when (row) {
            is DebugTaskSwitchRow -> copy(
                result = when (row.result) {
                    DebugTaskCheckResult.PASS -> true
                    DebugTaskCheckResult.FAIL -> false
                    DebugTaskCheckResult.PENDING -> null
                },
            )
            is DebugTaskValueRow -> copy(result = row.inputValue.takeIf { it.isNotBlank() })
            is DebugTaskTextRow -> copy(result = row.remark.takeIf { it.isNotBlank() })
            is DebugTaskPhotoRow -> copy(
                result = row.photos.joinToString(",") { it.fileId }.takeIf { it.isNotBlank() },
            )
            null -> this
        }
    }

    private fun String.toPhotoItems(): List<DebugTaskPhotoItem> {
        return split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { DebugTaskPhotoItem(fileId = it) }
            .take(MAX_PHOTO_COUNT)
    }

    private fun String.toDraftNumberText(): String {
        if (isBlank()) return this
        return toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: this
    }

    private fun DebugCheckItemDto.buildReferenceText(): String {
        return when {
            minValue != null && maxValue != null -> "范围：$minValue - $maxValue"
            minValue != null -> "最小值：$minValue"
            maxValue != null -> "最大值：$maxValue"
            !standardOption.isNullOrBlank() -> "参考值：$standardOption"
            else -> ""
        }
    }

    /**
     * 后端历史结果可能是 Boolean、数字字符串或英文状态，这里统一转成页面三态。
     */
    private fun Any?.toInitialSwitchResult(): DebugTaskCheckResult {
        return when (this) {
            true -> DebugTaskCheckResult.PASS
            false -> DebugTaskCheckResult.FAIL
            is String -> when {
                equals("true", true) || equals("pass", true) || equals("ok", true) || this == "1" ->
                    DebugTaskCheckResult.PASS
                equals("false", true) || equals("fail", true) || equals("ng", true) || this == "0" ->
                    DebugTaskCheckResult.FAIL
                else -> DebugTaskCheckResult.PENDING
            }
            else -> DebugTaskCheckResult.PENDING
        }
    }

    private fun String.toInitialValueResult(minValue: Int?, maxValue: Int?): DebugTaskCheckResult {
        if (isBlank()) return DebugTaskCheckResult.PENDING
        val number = toBigDecimalOrNull() ?: return DebugTaskCheckResult.FAIL
        minValue?.let {
            if (number < it.toBigDecimal()) return DebugTaskCheckResult.FAIL
        }
        maxValue?.let {
            if (number > it.toBigDecimal()) return DebugTaskCheckResult.FAIL
        }
        return DebugTaskCheckResult.PASS
    }

    private fun String.toTextResult(expectedText: String): DebugTaskCheckResult {
        if (isBlank()) return DebugTaskCheckResult.PENDING
        return if (DebugTaskInputRules.isInputMatched(this, expectedText)) {
            DebugTaskCheckResult.PASS
        } else {
            DebugTaskCheckResult.FAIL
        }
    }

    private fun Int.statusText(): String {
        return when (this) {
            1 -> "待调试"
            2 -> "调试中"
            3 -> "已完工"
            else -> "未知"
        }
    }

    private fun String?.orDash(): String {
        return this?.takeIf { it.isNotBlank() } ?: "-"
    }

    private enum class ActiveOperation {
        NONE,
        WAITING_NG,
        SAVE_DRAFT,
        SUBMIT_SN,
        FINISH_BATCH,
    }

    companion object {
        private const val EXTRA_TASK_ITEM_ID = "extra_task_item_id"
        private const val HEADER_ITEM_COUNT = 1
        private const val PAYLOAD_CHECK_CHANGED = "debugTaskCheckChanged"
        private const val MENU_SYNC = 1
        private const val MENU_FINISH_BATCH = 2
        private const val SUBMIT_DEBOUNCE_WINDOW = 800L
        private const val MAX_PHOTO_COUNT = 9

        fun start(context: Context, taskItemId: String?) {
            if (taskItemId.isNullOrBlank()) {
                JToast.show("批次ID为空")
                return
            }
            context.startActivity(createIntent(context, taskItemId))
        }

        fun createIntent(context: Context, taskItemId: String): Intent {
            return Intent(context, DebugTaskExecuteActivity::class.java)
                .putExtra(EXTRA_TASK_ITEM_ID, taskItemId)
        }
    }
}
