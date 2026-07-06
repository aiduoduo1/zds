package com.jj.zds.feature.repair.ui

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import coil.load
import com.jj.core.permission.hasJiangPermission
import com.jj.storage.JiangStorage
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.dialog.JSingleChoiceDialog
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.ui.toolbar.JiangToolbarMenuItem
import com.jj.ui.widget.JStatusType
import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import com.jj.zds.data.dto.repair.RepairAction
import com.jj.zds.data.dto.repair.RepairFileDto
import com.jj.zds.data.dto.repair.RepairSnScanValidDto
import com.jj.zds.data.dto.repair.RepairSubmitRequest
import com.jj.zds.data.local.ZdsStorageKeys
import com.jj.zds.databinding.ActivityRepairBinding
import com.jj.zds.feature.photo.PhotoUploadFileProcessor
import com.jj.zds.feature.photo.PhotoUploadMode
import com.jj.zds.feature.recheck.ui.RecheckActivity
import com.jj.zds.feature.repair.data.RepairUiState
import com.jj.zds.feature.repair.data.buildRepairFileUrl
import com.jj.zds.viewmodel.ZdsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class RepairActivity : JiangToolbarStateVmActivity<ActivityRepairBinding, ZdsViewModel>() {

    private var snCode: String = ""
    private var lastSubmitCode: String = ""
    private var lastSubmitAt: Long = 0L
    private var scanInProgress = false
    private var submitAction: RepairAction? = null
    private var submitAllowsRecheck = false
    private var submittedQrCode = ""
    private var currentState: RepairUiState? = null
    private var ngReasons = emptyList<DebugDictItemDto>()
    private var repairResults = emptyList<DebugDictItemDto>()
    private var selectedNgReason: String = ""
    private var selectedRepairResult: String = ""
    private var uploadingRepairPhoto = false
    private var pendingPhotoFile: File? = null
    private var repairPhotoUploadMode = PhotoUploadMode.default()
    private var clearingRepairPage = false
    private var showingRepairForm = false
    private var repairRemarkExpanded = false
    private var repairRemarkCanExpand = false
    private var moreActionView: TextView? = null
    private val repairPhotos = mutableListOf<RepairPhotoItem>()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchRepairPhotoCamera()
        } else {
            JToast.show("未授予相机权限，无法拍照")
        }
    }

    private val takeRepairPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingPhotoFile
        if (success && file != null) {
            uploadingRepairPhoto = true
            renderButtons()
            val uploadFile = PhotoUploadFileProcessor.prepareUploadFile(
                source = file,
                mode = repairPhotoUploadMode,
                cacheDir = cacheDir,
                prefix = "repair",
            )
            viewModel.uploadRepairPhoto(uploadFile)
        } else {
            pendingPhotoFile = null
            JToast.show("未完成拍照")
        }
    }

    override fun createViewBinding(inflater: LayoutInflater): ActivityRepairBinding {
        return ActivityRepairBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        installMoreAction()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (showingRepairForm) {
                        showSummaryPage()
                    } else {
                        finish()
                    }
                }
            },
        )
        bindScanInput()
        binding.btnStartRepair.setOnClickListener { showRepairForm() }
        binding.btnBackToSummary.setOnClickListener { showSummaryPage() }
        binding.btnToggleRemark.setOnClickListener { toggleRepairRemark() }
        binding.tvNgReason.setOnClickListener { showNgReasonDialog() }
        binding.tvRepairResult.setOnClickListener { showRepairResultDialog() }
        binding.btnTakePhoto.setOnClickListener { requestCameraAndTakePhoto() }
        binding.groupRepairPhotoUploadMode.setOnCheckedChangeListener { _, checkedId ->
            repairPhotoUploadMode = if (checkedId == binding.rbRepairPhotoOriginal.id) {
                PhotoUploadMode.ORIGINAL
            } else {
                PhotoUploadMode.COMPRESSED
            }
        }
        binding.btnSave.setOnClickListener { submitRepair(RepairAction.DRAFT) }
        binding.btnSubmit.setOnClickListener { submitRepair(RepairAction.SUBMIT) }
        viewModel.loadRepairNgReasons()
        viewModel.loadRepairResults()
        showSummaryPage()
        focusScanInput(selectAll = false)
    }

    override fun initPageObserver() {

        viewModel.repairSnScanValidResult.observe(this) { result ->
            scanInProgress = false
            renderRepair(RepairUiState.from(result))
        }
        viewModel.repairNgReasonItems.observe(this) { items ->
            ngReasons = items.filter { it.isShow }
        }
        viewModel.repairResultItems.observe(this) { items ->
            repairResults = items.filter { it.isShow }
            renderRepairResult()
        }
        viewModel.repairOperationDone.observe(this) {
            val action = submitAction
            val allowsRecheck = submitAllowsRecheck
            val handoffQrCode = submittedQrCode
            submitAction = null
            submitAllowsRecheck = false
            submittedQrCode = ""
            when {
                action == RepairAction.DRAFT -> {
                    JToast.show("维修记录已保存")
                    clearRepairPage()
                }
                action == RepairAction.SUBMIT && allowsRecheck -> {
                    clearRepairPage()
                    RecheckActivity.start(this, handoffQrCode)
                }
                action == RepairAction.SUBMIT -> {
                    JToast.show("维修结果已提交")
                    clearRepairPage()
                }
                else -> renderButtons()
            }
        }
        viewModel.repairPhotoUploadResult.observe(this) { fileId ->
            val file = pendingPhotoFile
            uploadingRepairPhoto = false
            pendingPhotoFile = null
            repairPhotos += RepairPhotoItem(fileId = fileId.toString(), localFile = file)
            renderPhotos()
            renderButtons()
            JToast.show("维修照片已上传")
        }
    }

    override fun toolbarTitle(): CharSequence = "不良维修"

    override fun toolbarSubtitle(): CharSequence = "扫描 SN 处理未关闭不良"

    override fun toolbarMenuItems(): List<JiangToolbarMenuItem> {
        return listOf(JiangToolbarMenuItem(MENU_SYNC, "刷新"))
    }

    override fun showBackButton(): Boolean = true

    override fun onToolbarBackClick() {
        if (showingRepairForm) {
            showSummaryPage()
        } else {
            finish()
        }
    }

    override fun onToolbarMenuClick(item: JiangToolbarMenuItem) {
        if (item.id == MENU_SYNC) {
            lastSubmitCode = ""
            submitScanSn(binding.inputSn.text())
            viewModel.loadRepairNgReasons()
            viewModel.loadRepairResults()
        }
    }

    private fun installMoreAction() {
        val action = TextView(this).apply {
            gravity = Gravity.CENTER
            text = "⋮"
            textSize = 24f
            setTextColor(getColor(com.jj.ui.R.color.j_color_text_white))
            contentDescription = "更多操作"
            setOnClickListener { showMoreMenu(this) }
        }
        moreActionView = action
        toolbar.addView(
            action,
            LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }

    private fun showMoreMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, MENU_DETAIL, 0, "查看详情信息")
            menu.add(0, MENU_HISTORY, 1, "查看处理记录")
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    MENU_DETAIL -> withCurrentReport { RepairDetailActivity.start(this@RepairActivity, it) }
                    MENU_HISTORY -> withCurrentReport { RepairHistoryActivity.start(this@RepairActivity, it) }

                }
                true
            }
            show()
        }
    }

    private inline fun withCurrentReport(action: (RepairSnScanValidDto) -> Unit) {
        val report = currentState?.report
        if (report == null) {
            JToast.show("请先扫描 SN")
        } else {
            action(report)
        }
    }

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        if (scanInProgress) {
            scanInProgress = false
            binding.inputSn.showError(message).selectAll()
            focusScanInput(selectAll = true)
            return
        }
        val action = submitAction
        submitAction = null
        submitAllowsRecheck = false
        submittedQrCode = ""
        if (uploadingRepairPhoto) {
            uploadingRepairPhoto = false
            pendingPhotoFile = null
        }
        renderButtons()
        if (action != null) {
            showRepairSubmitError(message)
            return
        }
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
            if (!clearingRepairPage && it.isBlank() && currentState != null) {
                resetRepairContent()
            }
        }
        binding.inputSn.onInputDone {
            submitScanSn(it)
        }
        binding.inputSn.onTextDebounce(delayMillis = 260L) {
            submitScanSn(it)
        }
    }

    private fun clearRepairPage() {
        clearingRepairPage = true
        binding.inputSn.setText("")
        clearingRepairPage = false
        resetRepairContent()
        focusScanInput(selectAll = false)
    }

    private fun resetRepairContent() {
        snCode = ""
        lastSubmitCode = ""
        lastSubmitAt = 0L
        scanInProgress = false
        submitAction = null
        submitAllowsRecheck = false
        submittedQrCode = ""
        currentState = null
        selectedNgReason = ""
        selectedRepairResult = ""
        uploadingRepairPhoto = false
        pendingPhotoFile = null
        repairPhotos.clear()
        binding.inputSn.clearState()
        repairRemarkExpanded = false
        repairRemarkCanExpand = false
        binding.btnToggleRemark.visibility = View.GONE
        showSummaryPage()
        renderButtons()
    }

    private fun showSummaryPage() {
        showingRepairForm = false
        binding.layoutScanCard.visibility = View.VISIBLE
        binding.layoutRepairForm.visibility = View.GONE
        binding.layoutSummaryPage.visibility = if (currentState == null) View.GONE else View.VISIBLE
        binding.tvEmpty.visibility = if (currentState == null) View.VISIBLE else View.GONE
        toolbar.setTitleText("不良维修")
        toolbar.setSubtitleText("扫描 SN 处理未关闭不良")
        toolbar.setMenuItemVisible(MENU_SYNC, true)
        moreActionView?.visibility = View.VISIBLE
        binding.scrollContent.post { binding.scrollContent.scrollTo(0, 0) }
        if (currentState == null) {
            focusScanInput(selectAll = false)
        }
    }

    private fun showRepairForm() {
        val state = currentState ?: run {
            JToast.show("请先扫描 SN")
            return
        }
        if (!state.editable) {
            JToast.show("当前记录不可维修")
            return
        }
        showingRepairForm = true
        binding.layoutScanCard.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.layoutSummaryPage.visibility = View.GONE
        binding.layoutRepairForm.visibility = View.VISIBLE
        toolbar.setTitleText("填写维修结果")
        toolbar.setSubtitleText(state.report.sn.orDash())
        toolbar.setMenuItemVisible(MENU_SYNC, false)
        moreActionView?.visibility = View.GONE
        binding.scrollContent.post { binding.scrollContent.scrollTo(0, 0) }
    }

    private fun focusScanInput(selectAll: Boolean) {
        binding.inputSn.post {
            binding.inputSn.requestInputFocus(showKeyboard = false)
            if (selectAll) {
                binding.inputSn.selectAll()
            }
        }
    }

    private fun showRepairSubmitError(message: String) {
        JConfirmDialog(this, this)
            .setTitle("操作失败")
            .setMessage(message.ifBlank { "维修记录保存失败，请稍后重试" })
            .setConfirmText("知道了")
            .setShowCancelButton(false)
            .show()
    }

    private fun showFinalStateSubmitDialog(request: RepairSubmitRequest) {
        JConfirmDialog(this, this)
            .setTitle("确认提交")
            .setMessage("该状态为最终状态，确认后无法继续使用，是否确认？")
            .setCancelText("取消")
            .setConfirmText("确认")
            .setCancelable(false)
            .setOnConfirm {
                dispatchRepair(request, RepairAction.SUBMIT, allowsRecheck = false)
            }
            .show()
    }

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
        viewModel.scanValidRepairSn(code)
    }

    private fun renderRepair(state: RepairUiState) {
        currentState = state
        val report = state.report
        selectedNgReason = report.ngReason.orEmpty()
        selectedRepairResult = report.result.orEmpty()
        repairPhotos.clear()
        repairPhotos += report.fileList
            .take(MAX_PHOTO_COUNT)
            .mapNotNull { file ->
                file.fileId.takeIf { it.isNotBlank() }?.let { RepairPhotoItem(fileId = it) }
            }
        binding.inputSn.showSuccess("已带出不良记录")
        binding.tagStatus.setStatus(report.statusName.orStatus(), report.toTagType(state.editable))
        binding.tvSn.text = report.sn.orDash()
        binding.tvNgPhenomena.text = report.ngPhenomena.orDash()
        binding.tvFormSn.text = "${report.sn.orDash()} · ${report.ngPhenomena.orDash()}"
        renderRepairRemark(RepairRemarkDisplay.format(report.ngRemark).orDash())
        renderRepairResult()
        binding.tvNgReason.text = selectedNgReason.ifBlank { "请选择不良原因" }
        binding.editRepairBy.setText(
            report.repairBy?.takeIf { it.isNotBlank() }
                ?: JiangStorage.kv.getString(ZdsStorageKeys.USER_NAME, "").orEmpty(),
        )
        binding.editRepairDesc.setText(report.repairDesc.orEmpty())
        renderPhotos()
        setEditable(state.editable)
        showSummaryPage()
    }

    private fun renderRepairRemark(remark: String) {
        repairRemarkExpanded = false
        repairRemarkCanExpand = false
        binding.tvNgRemark.maxLines = Int.MAX_VALUE
        binding.tvNgRemark.ellipsize = null
        binding.tvNgRemark.text = remark
        binding.btnToggleRemark.visibility = View.GONE
        binding.btnToggleRemark.text = "展开"
        binding.tvNgRemark.post {
            repairRemarkCanExpand = binding.tvNgRemark.lineCount > COLLAPSED_REMARK_LINES
            if (repairRemarkCanExpand && !repairRemarkExpanded) {
                binding.tvNgRemark.maxLines = COLLAPSED_REMARK_LINES
                binding.tvNgRemark.ellipsize = TextUtils.TruncateAt.END
                binding.btnToggleRemark.visibility = View.VISIBLE
            }
        }
    }

    private fun toggleRepairRemark() {
        if (!repairRemarkCanExpand) return
        repairRemarkExpanded = !repairRemarkExpanded
        binding.tvNgRemark.maxLines = if (repairRemarkExpanded) Int.MAX_VALUE else COLLAPSED_REMARK_LINES
        binding.tvNgRemark.ellipsize = if (repairRemarkExpanded) null else TextUtils.TruncateAt.END
        binding.btnToggleRemark.text = if (repairRemarkExpanded) "收起" else "展开"
    }

    private fun setEditable(editable: Boolean) {
        binding.btnStartRepair.isEnabled = editable
        binding.btnStartRepair.alpha = if (editable) 1f else 0.6f
        binding.btnStartRepair.text = if (editable) "填写维修结果" else "当前记录不可维修"
        binding.tvRepairResult.isEnabled = editable
        binding.tvNgReason.isEnabled = editable
        binding.editRepairBy.isEnabled = editable
        binding.editRepairDesc.isEnabled = editable
        binding.btnTakePhoto.isEnabled = editable
        binding.groupRepairPhotoUploadMode.isEnabled = editable
        binding.rbRepairPhotoCompressed.isEnabled = editable
        binding.rbRepairPhotoOriginal.isEnabled = editable
        renderButtons()
    }

    private fun renderButtons() {
        val editable = currentState?.editable == true
        val submitting = submitAction != null
        binding.btnTakePhoto.isEnabled = editable && !submitting && !uploadingRepairPhoto &&
            repairPhotos.size < MAX_PHOTO_COUNT
        binding.btnTakePhoto.alpha = if (binding.btnTakePhoto.isEnabled) 1f else 0.6f
        binding.groupRepairPhotoUploadMode.alpha = if (binding.btnTakePhoto.isEnabled) 1f else 0.6f
        binding.groupRepairPhotoUploadMode.isEnabled = binding.btnTakePhoto.isEnabled
        binding.rbRepairPhotoCompressed.isEnabled = binding.btnTakePhoto.isEnabled
        binding.rbRepairPhotoOriginal.isEnabled = binding.btnTakePhoto.isEnabled
        binding.btnTakePhoto.text = when {
            uploadingRepairPhoto -> "上传中..."
            repairPhotos.size >= MAX_PHOTO_COUNT -> "最多9张"
            else -> "拍照上传"
        }
        binding.btnSave.isEnabled = editable && !submitting && !uploadingRepairPhoto
        binding.btnSubmit.isEnabled = editable && !submitting && !uploadingRepairPhoto
        binding.btnSave.alpha = if (binding.btnSave.isEnabled) 1f else 0.6f
        binding.btnSubmit.alpha = if (binding.btnSubmit.isEnabled) 1f else 0.6f
        binding.btnSave.text = if (submitAction == RepairAction.DRAFT) "保存中..." else "保存"
        binding.btnSubmit.text = if (submitAction == RepairAction.SUBMIT) "提交中..." else "提交"
    }

    private fun showNgReasonDialog() {
        if (ngReasons.isEmpty()) {
            JToast.show("不良原因列表为空")
            return
        }
        JSingleChoiceDialog<DebugDictItemDto>(this, this)
            .setTitle("选择不良原因")
            .setItems(ngReasons) { it.label ?: it.value ?: it.id.orDash() }
            .setOnConfirm { selected ->
                val value = selected?.value ?: selected?.label
                if (value.isNullOrBlank()) {
                    JToast.show("请选择不良原因")
                } else {
                    selectedNgReason = value
                    binding.tvNgReason.text = value
                }
            }
            .show()
    }

    private fun showRepairResultDialog() {
        if (repairResults.isEmpty()) {
            JToast.show("维修结果列表为空")
            return
        }
        JSingleChoiceDialog<DebugDictItemDto>(this, this)
            .setTitle("选择维修结果")
            .setItems(repairResults) { it.label ?: it.value ?: it.id.orDash() }
            .setOnConfirm { selected ->
                val value = selected?.value ?: selected?.label
                if (value.isNullOrBlank()) {
                    JToast.show("请选择维修结果")
                } else {
                    selectedRepairResult = value
                    renderRepairResult()
                }
            }
            .show()
    }

    private fun renderRepairResult() {
        binding.tvRepairResult.text = RepairResultDisplay
            .labelFor(selectedRepairResult, repairResults)
            .ifBlank { "请选择维修结果" }
    }

    private fun submitRepair(action: RepairAction) {
        val state = currentState ?: run {
            JToast.show("请先扫描 SN")
            return
        }
        val result = selectedRepairResult.trim()
        val ngReason = selectedNgReason.trim()
        val repairBy = binding.editRepairBy.text?.toString()?.trim().orEmpty()
        val repairDesc = binding.editRepairDesc.text?.toString()?.trim().orEmpty()
        val requireCompleteInput = action == RepairAction.SUBMIT
        when {
            uploadingRepairPhoto -> {
                JToast.show("照片上传中，请稍后操作")
                return
            }
            state.submitId <= 0L -> {
                JToast.show("不良上报ID为空")
                return
            }
            requireCompleteInput && result.isBlank() -> {
                JToast.show("请选择维修结果")
                return
            }
            requireCompleteInput && ngReason.isBlank() -> {
                JToast.show("请选择不良原因")
                return
            }
            requireCompleteInput && repairBy.isBlank() -> {
                JToast.show("请填写维修人")
                return
            }
            requireCompleteInput && repairDesc.isBlank() -> {
                JToast.show("请填写维修说明")
                return
            }
        }
        val request = RepairSubmitRequest(
            action = action.value,
            id = state.submitId,
            result = result,
            ngReason = ngReason,
            repairBy = repairBy,
            repairDesc = repairDesc,
            fileList = repairPhotos
                .map { it.fileId }
                .filter { it.isNotBlank() }
                .take(MAX_PHOTO_COUNT)
                .map { RepairFileDto(fileId = it) },
        )
        val allowsRecheck = action == RepairAction.SUBMIT &&
            RepairSubmitSuccessRules.isRecheckAllowed(result, repairResults)
        if (action == RepairAction.SUBMIT && !allowsRecheck) {
            showFinalStateSubmitDialog(request)
            return
        }
        dispatchRepair(request, action, allowsRecheck)
    }

    private fun dispatchRepair(
        request: RepairSubmitRequest,
        action: RepairAction,
        allowsRecheck: Boolean,
    ) {
        submitAction = action
        submitAllowsRecheck = allowsRecheck
        submittedQrCode = if (allowsRecheck) {
            RepairSubmitSuccessRules.handoffQrCode(binding.inputSn.text())
        } else {
            ""
        }
        renderButtons()
        viewModel.submitRepair(request)
    }

    private fun requestCameraAndTakePhoto() {
        if (repairPhotos.size >= MAX_PHOTO_COUNT) {
            JToast.show("维修照片最多9张")
            return
        }
        if (hasJiangPermission(Manifest.permission.CAMERA)) {
            launchRepairPhotoCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchRepairPhotoCamera() {
        val file = createRepairPhotoFile()
        pendingPhotoFile = file
        takeRepairPhotoLauncher.launch(file.toProviderUri())
    }

    private fun createRepairPhotoFile(): File {
        val dir = File(cacheDir, "repair_photo").apply {
            mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        return File(dir, "repair_${currentState?.submitId ?: 0L}_$timestamp.jpg")
    }

    private fun File.toProviderUri() = FileProvider.getUriForFile(
        this@RepairActivity,
        "${packageName}.jiang.fileprovider",
        this,
    )

    private fun renderPhotos() {
        val photos = repairPhotos.filter { it.fileId.isNotBlank() }
        binding.tvPhoto.text = "维修照片：${photos.size} 张"
        binding.layoutPhotoList.removeAllViews()
        binding.scrollPhotoList.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
        photos.forEachIndexed { index, photo ->
            binding.layoutPhotoList.addView(createPhotoView(photo, index, allowDelete = true))
        }
    }

    private fun createPhotoView(photo: RepairPhotoItem, index: Int, allowDelete: Boolean): View {
        val container = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(92), dp(92)).apply {
                marginEnd = dp(8)
            }
        }
        val imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "维修照片 ${photo.fileId}"
            setBackgroundResource(com.jj.ui.R.drawable.j_bg_input)
            loadRepairPhoto(photo)
            setOnClickListener { showPhotoPreview(photo) }
        }
        container.addView(imageView)
        if (allowDelete && currentState?.editable == true && !uploadingRepairPhoto && submitAction == null) {
            val deleteView = TextView(this).apply {
                layoutParams = FrameLayout.LayoutParams(dp(24), dp(24), Gravity.TOP or Gravity.END)
                gravity = Gravity.CENTER
                text = "×"
                textSize = 18f
                setTextColor(Color.WHITE)
                setBackgroundColor(0x99000000.toInt())
                contentDescription = "删除维修照片"
                setOnClickListener { deleteRepairPhoto(index) }
            }
            container.addView(deleteView)
        }
        return container
    }

    private fun showPhotoGallery() {
        val photos = repairPhotos.filter { it.fileId.isNotBlank() }
        if (photos.isEmpty()) {
            JToast.show("暂无维修照片")
            return
        }
        val popupRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(28), dp(16), dp(24))
            setBackgroundColor(0xF0000000.toInt())
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val title = TextView(this).apply {
            text = "维修照片  ${photos.size} 张"
            textSize = 18f
            setTextColor(Color.WHITE)
        }
        val close = TextView(this).apply {
            gravity = Gravity.CENTER
            text = "×"
            textSize = 30f
            setTextColor(Color.WHITE)
            contentDescription = "关闭维修照片"
        }
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(44), dp(44)))
        val photoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(12))
        }
        photos.forEachIndexed { index, photo ->
            photoRow.addView(createPhotoView(photo, index, allowDelete = false))
        }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(photoRow)
        }
        val hint = TextView(this).apply {
            gravity = Gravity.CENTER
            text = "点击照片查看大图"
            textSize = 14f
            setTextColor(0xFFCBD5E1.toInt())
        }
        popupRoot.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        popupRoot.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        popupRoot.addView(hint, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val popupWindow = PopupWindow(
            popupRoot,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true,
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        close.setOnClickListener { popupWindow.dismiss() }
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
    }

    private fun ImageView.loadRepairPhoto(photo: RepairPhotoItem) {
        val file = photo.localFile?.takeIf { it.exists() }
        if (file != null) {
            load(file)
        } else {
            val url = buildRepairFileUrl(photo.fileId, currentToken())
            if (url.isNullOrBlank()) {
                JToast.show("图片地址为空")
            } else {
                load(url)
            }
        }
    }

    private fun showPhotoPreview(photo: RepairPhotoItem) {
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
            contentDescription = "维修照片大图 ${photo.fileId}"
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
        popupRoot.post { imageView.loadRepairPhoto(photo) }
    }

    private fun deleteRepairPhoto(index: Int) {
        val photo = repairPhotos.getOrNull(index) ?: return
        photo.localFile?.delete()
        repairPhotos.removeAt(index)
        renderPhotos()
        renderButtons()
    }

    private fun currentToken(): String {
        return JiangStorage.kv.getString(ZdsStorageKeys.MES_UP_TOKEN, "").orEmpty()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun RepairSnScanValidDto.toTagType(editable: Boolean): JStatusType {
        return when {
            statusName.orEmpty().contains("待维修") -> JStatusType.WARNING
            !editable -> JStatusType.NEUTRAL
            status == 1 -> JStatusType.WARNING
            status == 2 -> JStatusType.INFO
            else -> JStatusType.NEUTRAL
        }
    }

    private fun String?.orDash(): String = takeIf { !it.isNullOrBlank() } ?: "-"

    private fun String?.orStatus(): String = takeIf { !it.isNullOrBlank() } ?: "未知状态"

    private companion object {
        const val MENU_SYNC = 1
        const val MENU_DETAIL = 2
        const val MENU_HISTORY = 3
        const val MENU_PHOTOS = 4
        const val MENU_CLEAR = 5
        const val SUBMIT_DEBOUNCE_WINDOW = 900L
        const val MAX_PHOTO_COUNT = 9
        const val COLLAPSED_REMARK_LINES = 3
    }

    private data class RepairPhotoItem(
        val fileId: String,
        val localFile: File? = null,
    )
}
