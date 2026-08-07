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
import androidx.core.widget.doAfterTextChanged
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
import com.jj.zds.data.dto.repair.ProductRepairPartDto
import com.jj.zds.data.dto.repair.REPAIR_FAULT_TYPE_KEY_PART
import com.jj.zds.data.dto.repair.REPAIR_FAULT_TYPE_OTHER_PART
import com.jj.zds.data.dto.repair.REPAIR_MAINTAIN_TYPE_CHANGE_PART
import com.jj.zds.data.dto.repair.REPAIR_MAINTAIN_TYPE_NORMAL
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
    private var productRepairParts = emptyList<ProductRepairPartDto>()
    private var productFaultSelection = ProductFaultSelection()
    private var productRepairMode = PRODUCT_MODE_NORMAL
    private var syncingFaultMaterialInput = false
    private var syncingReplacementInput = false
    private var replacementScanInProgress = false
    private var pendingReplacementQrCode = ""
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
        binding.groupRepairMode.setOnCheckedChangeListener { _, checkedId ->
            productRepairMode = if (checkedId == binding.rbReplaceRepair.id) {
                PRODUCT_MODE_REPLACE
            } else {
                PRODUCT_MODE_NORMAL
            }
            clearUnrepairableResultForReplace(showToast = true)
            renderProductRepairContext()
        }
        binding.editFaultMaterialCode.doAfterTextChanged { editable ->
            if (syncingFaultMaterialInput) return@doAfterTextChanged
            val code = editable?.toString().orEmpty()
            if (code != productFaultSelection.materialCode) {
                productFaultSelection = productFaultSelection.enterMaterialCode(code)
                renderKeyPartOptions()
                renderProductRepairContext()
            }
        }
        binding.inputReplacementSn.onTextChanged { text ->
            if (syncingReplacementInput) return@onTextChanged
            if (text.trim() != productFaultSelection.replacementSn) {
                productFaultSelection = productFaultSelection.selectReplacement("")
                pendingReplacementQrCode = ""
                binding.inputReplacementSn.clearState()
            }
        }
        binding.inputReplacementSn.onInputDone { submitReplacementSn(it) }
        binding.inputReplacementSn.onTextDebounce(delayMillis = 260L) { submitReplacementSn(it) }
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
            clearUnrepairableResultForReplace(showToast = false)
            renderRepairResult()
        }
        viewModel.productRepairParts.observe(this) { parts ->
            productRepairParts = parts
            hydrateSelectedKeyPartCategory()
            renderProductRepairContext()
        }
        viewModel.newRepairPartSnScanValidResult.observe(this) { result ->
            replacementScanInProgress = false
            if (!isProductRepair()) return@observe
            val sn = result.sn.orEmpty()
            if (sn.isBlank()) {
                binding.inputReplacementSn.showError("新关键部件 SN 为空").selectAll()
                return@observe
            }
            productFaultSelection = productFaultSelection.selectReplacement(
                sn = sn,
                qrCode = pendingReplacementQrCode,
            )
            setReplacementSnText(sn)
            binding.inputReplacementSn.showSuccess("新关键部件校验通过")
            renderProductRepairContext()
        }
        viewModel.repairOperationDone.observe(this) {
            val action = submitAction
            val allowsRecheck = submitAllowsRecheck
            val handoffQrCode = submittedQrCode
            submitAction = null
            submitAllowsRecheck = false
            submittedQrCode = ""
            handleRepairDone(action, allowsRecheck, handoffQrCode)
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

    private fun handleRepairDone(
        action: RepairAction?,
        allowsRecheck: Boolean,
        handoffQrCode: String,
    ) {
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

    override fun toolbarTitle(): CharSequence = "不良维修"

    override fun toolbarSubtitle(): CharSequence = "扫描 SN/PRO 码处理未关闭不良"

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
            JToast.show("请先扫描 SN 或 PRO 码")
        } else {
            action(report)
        }
    }

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        if (replacementScanInProgress) {
            replacementScanInProgress = false
            pendingReplacementQrCode = ""
            binding.inputReplacementSn.showError(message).selectAll()
            binding.inputReplacementSn.requestInputFocus(showKeyboard = false)
            renderButtons()
            return
        }
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

    private fun setFaultMaterialText(text: String) {
        syncingFaultMaterialInput = true
        binding.editFaultMaterialCode.setText(text)
        binding.editFaultMaterialCode.setSelection(binding.editFaultMaterialCode.text?.length ?: 0)
        syncingFaultMaterialInput = false
    }

    private fun setReplacementSnText(text: String) {
        syncingReplacementInput = true
        binding.inputReplacementSn.setText(text)
        syncingReplacementInput = false
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
        productFaultSelection = ProductFaultSelection()
        productRepairMode = PRODUCT_MODE_NORMAL
        productRepairParts = emptyList()
        replacementScanInProgress = false
        pendingReplacementQrCode = ""
        setFaultMaterialText("")
        setReplacementSnText("")
        binding.rbNormalRepair.isChecked = true
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
        toolbar.setSubtitleText("扫描 SN/PRO 码处理未关闭不良")
        toolbar.setMenuItemVisible(MENU_SYNC, true)
        moreActionView?.visibility = View.VISIBLE
        binding.scrollContent.post { binding.scrollContent.scrollTo(0, 0) }
        if (currentState == null) {
            focusScanInput(selectAll = false)
        }
    }

    private fun showRepairForm() {
        val state = currentState ?: run {
            JToast.show("请先扫描 SN 或 PRO 码")
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
        binding.inputSn.verifying("SN/PRO 码校验中...")
        viewModel.scanValidRepairSn(code)
    }

    private fun submitReplacementSn(rawCode: String) {
        val code = rawCode.trim()
        val categoryId = productFaultSelection.keyPartCategoryId.trim()
        if (code.isBlank()) return
        if (replacementScanInProgress) return
        if (code == productFaultSelection.replacementSn.trim()) {
            return
        }
        if (categoryId.isBlank()) {
            JToast.show("请先选择要更换的关键部件")
            return
        }
        if (code == productFaultSelection.keyPartSn.trim()) {
            binding.inputReplacementSn.showError("新旧关键部件 SN 不能相同").selectAll()
            return
        }
        replacementScanInProgress = true
        pendingReplacementQrCode = code
        binding.inputReplacementSn.verifying("新关键部件校验中...")
        viewModel.scanValidNewRepairPartSn(categoryId, code)
    }

    private fun renderRepair(state: RepairUiState) {
        currentState = state
        val report = state.report
        selectedNgReason = report.ngReason.orEmpty()
        selectedRepairResult = report.result.orEmpty()
        productRepairParts = emptyList()
        productFaultSelection = report.toProductFaultSelection()
        productRepairMode = report.maintainType
            ?.takeIf { it == PRODUCT_MODE_REPLACE || it == PRODUCT_MODE_NORMAL }
            ?: PRODUCT_MODE_NORMAL
        replacementScanInProgress = false
        pendingReplacementQrCode = ""
        setFaultMaterialText(productFaultSelection.materialCode)
        setReplacementSnText(productFaultSelection.replacementSn)
        if (productRepairMode == PRODUCT_MODE_REPLACE) {
            binding.rbReplaceRepair.isChecked = true
        } else {
            binding.rbNormalRepair.isChecked = true
        }
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
        clearUnrepairableResultForReplace(showToast = false)
        renderRepairResult()
        binding.tvNgReason.text = selectedNgReason.ifBlank { "请选择不良原因" }
        binding.editRepairBy.setText(
            report.repairBy?.takeIf { it.isNotBlank() }
                ?: JiangStorage.kv.getString(ZdsStorageKeys.USER_NAME, "").orEmpty(),
        )
        binding.editRepairDesc.setText(report.repairDesc.orEmpty())
        if (ProductRepairRules.isFinishedProductSource(report.ngSource)) {
            viewModel.loadProductRepairParts(state.submitId)
        }
        renderProductRepairContext()
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
        binding.rbNormalRepair.isEnabled = editable
        binding.rbReplaceRepair.isEnabled = editable
        binding.editFaultMaterialCode.isEnabled = editable
        binding.inputReplacementSn.isEnabled = editable
        renderButtons()
    }

    private fun renderProductRepairContext() {
        val report = currentState?.report
        val isProduct = ProductRepairRules.isFinishedProductSource(report?.ngSource)
        binding.layoutProductRepair.visibility = if (isProduct) View.VISIBLE else View.GONE
        if (!isProduct) return
        binding.rbReplaceRepair.visibility = View.VISIBLE
        renderKeyPartOptions()
        val showReplacementScan = productRepairMode == PRODUCT_MODE_REPLACE &&
            productFaultSelection.keyPartSn.isNotBlank()
        binding.inputReplacementSn.visibility = if (showReplacementScan) View.VISIBLE else View.GONE
        if (!showReplacementScan) {
            productFaultSelection = productFaultSelection.selectReplacement("")
            pendingReplacementQrCode = ""
            setReplacementSnText("")
            binding.inputReplacementSn.clearState()
        }
    }

    private fun hydrateSelectedKeyPartCategory() {
        val selectedSn = productFaultSelection.keyPartSn.trim()
        if (selectedSn.isBlank() || productFaultSelection.keyPartCategoryId.isNotBlank()) return
        val part = productRepairParts.firstOrNull { it.materialCode?.trim() == selectedSn }
        val categoryId = part?.categoryId?.trim().orEmpty()
        if (categoryId.isNotBlank()) {
            productFaultSelection = productFaultSelection.selectKeyPart(selectedSn, categoryId)
            if (productFaultSelection.replacementSn.isBlank()) {
                productFaultSelection = productFaultSelection.selectReplacement(currentState?.report?.newPartSn.orEmpty())
            }
        }
    }

    private fun renderKeyPartOptions() {
        val report = currentState?.report ?: return
        if (!ProductRepairRules.isFinishedProductSource(report.ngSource)) return
        val editable = currentState?.editable == true
        binding.layoutKeyPartOptions.removeAllViews()
        if (productRepairParts.isEmpty()) {
            binding.layoutKeyPartOptions.addView(
                TextView(this).apply {
                    text = "暂无关键部件数据"
                    textSize = 14f
                    setTextColor(0xFF64748B.toInt())
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                },
            )
            return
        }
        productRepairParts.forEach { part ->
            val partSn = part.materialCode.orEmpty()
            val selected = productFaultSelection.keyPartSn == partSn
            val option = TextView(this).apply {
                text = listOf(part.productName, part.categoryName, partSn)
                    .filter { !it.isNullOrBlank() }
                    .joinToString("\n")
                    .ifBlank { partSn.ifBlank { "未命名关键部件" } }
                textSize = 15f
                setTextColor(if (selected) 0xFF1769E0.toInt() else 0xFF172033.toInt())
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setBackgroundResource(com.jj.ui.R.drawable.j_bg_input)
                isEnabled = editable && partSn.isNotBlank() && !part.categoryId.isNullOrBlank()
                alpha = if (selected) 1f else 0.82f
                setOnClickListener {
                    productFaultSelection = productFaultSelection.selectKeyPart(
                        sn = partSn,
                        categoryId = part.categoryId.orEmpty(),
                    )
                    setFaultMaterialText("")
                    setReplacementSnText("")
                    pendingReplacementQrCode = ""
                    binding.inputReplacementSn.clearState()
                    renderProductRepairContext()
                }
            }
            binding.layoutKeyPartOptions.addView(
                option,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(4)
                },
            )
        }
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
        val availableResults = availableRepairResults()
        if (availableResults.isEmpty()) {
            JToast.show("换件维修没有可选维修结果")
            return
        }
        JSingleChoiceDialog<DebugDictItemDto>(this, this)
            .setTitle("选择维修结果")
            .setItems(availableResults) { it.label ?: it.value ?: it.id.orDash() }
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

    private fun availableRepairResults(): List<DebugDictItemDto> {
        if (!isProductReplaceRepair()) return repairResults
        return repairResults.filterNot { ProductRepairRules.isUnrepairableResult(it.label, it.value) }
    }

    private fun clearUnrepairableResultForReplace(showToast: Boolean) {
        if (!isProductReplaceRepair()) return
        if (!isUnrepairableSelectedResult()) return
        selectedRepairResult = ""
        renderRepairResult()
        if (showToast) {
            JToast.show("换件维修不能选择修不好，请重新选择维修结果")
        }
    }

    private fun selectedRepairResultItem(): DebugDictItemDto? {
        val value = selectedRepairResult.trim()
        if (value.isBlank()) return null
        return repairResults.firstOrNull {
            it.value?.trim() == value || it.label?.trim() == value
        }
    }

    private fun submitRepair(action: RepairAction) {
        val state = currentState ?: run {
            JToast.show("请先扫描 SN 或 PRO 码")
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
            requireCompleteInput && isProductReplaceRepair() && isUnrepairableSelectedResult() -> {
                JToast.show("换件维修不能选择修不好，请重新选择维修结果")
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
        if (requireCompleteInput && isProductRepair()) {
            val productError = validateProductRepair()
            if (productError != null) {
                JToast.show(productError)
                return
            }
        }
        val request = RepairSubmitRequest(
            action = action.value,
            id = state.submitId,
            faultType = repairFaultType(),
            maintainType = repairMaintainType(),
            damagePartSn = repairDamagePartSn(),
            newPartSn = repairNewPartSn(),
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
        val outcome = ProductRepairRules.submitOutcome(state.report.ngSource, result, repairResults)
        val allowsRecheck = action == RepairAction.SUBMIT && outcome == RepairSubmitOutcome.RECHECK
        if (action == RepairAction.SUBMIT && outcome == RepairSubmitOutcome.FINAL_STATE) {
            showFinalStateSubmitDialog(request)
            return
        }
        dispatchRepair(request, action, allowsRecheck)
    }

    private fun isProductRepair(): Boolean {
        return ProductRepairRules.isFinishedProductSource(currentState?.report?.ngSource)
    }

    private fun isProductReplaceRepair(): Boolean {
        return isProductRepair() && productRepairMode == PRODUCT_MODE_REPLACE
    }

    private fun isUnrepairableSelectedResult(): Boolean {
        val selected = selectedRepairResultItem()
        return if (selected != null) {
            ProductRepairRules.isUnrepairableResult(selected.label, selected.value)
        } else {
            ProductRepairRules.isUnrepairableResult(selectedRepairResult, selectedRepairResult)
        }
    }

    private fun validateProductRepair(): String? {
        val keyPartSn = productFaultSelection.keyPartSn.trim()
        val materialCode = productFaultSelection.materialCode.trim()
        if (keyPartSn.isBlank() && materialCode.isBlank()) {
            return "请选择损坏关键部件或填写非关键部件物料号"
        }
        if (productRepairMode == PRODUCT_MODE_REPLACE) {
            if (keyPartSn.isBlank()) return "更换关键部件必须选择要更换的关键部件"
            if (productFaultSelection.keyPartCategoryId.isBlank()) return "所选关键部件缺少品类ID"
            val replacementSn = productFaultSelection.replacementSn.trim()
            if (replacementSn.isBlank()) return "请扫描新关键部件 SN 码"
            if (replacementSn == keyPartSn) return "新旧关键部件 SN 不能相同"
        }
        return null
    }

    private fun repairFaultType(): String {
        if (!isProductRepair()) return ""
        return if (productFaultSelection.keyPartSn.isNotBlank()) {
            REPAIR_FAULT_TYPE_KEY_PART
        } else {
            REPAIR_FAULT_TYPE_OTHER_PART
        }
    }

    private fun repairMaintainType(): String {
        return if (isProductRepair()) {
            productRepairMode
        } else {
            REPAIR_MAINTAIN_TYPE_NORMAL
        }
    }

    private fun repairDamagePartSn(): String {
        if (!isProductRepair()) return ""
        return productFaultSelection.keyPartSn.trim()
            .ifBlank { productFaultSelection.materialCode.trim() }
    }

    private fun repairNewPartSn(): String {
        if (!isProductRepair()) return ""
        if (productRepairMode != PRODUCT_MODE_REPLACE) return ""
        return productFaultSelection.replacementSn.trim()
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

    private fun RepairSnScanValidDto.toProductFaultSelection(): ProductFaultSelection {
        if (!ProductRepairRules.isFinishedProductSource(ngSource)) return ProductFaultSelection()
        val damageSn = damagePartSn.orEmpty()
        return if (faultType == REPAIR_FAULT_TYPE_OTHER_PART) {
            ProductFaultSelection(materialCode = damageSn)
        } else {
            ProductFaultSelection(
                keyPartSn = damageSn,
                keyPartCategoryId = damagePartCatId.orEmpty(),
                replacementSn = newPartSn.orEmpty(),
            )
        }
    }

    private companion object {
        const val MENU_SYNC = 1
        const val MENU_DETAIL = 2
        const val MENU_HISTORY = 3
        const val MENU_PHOTOS = 4
        const val MENU_CLEAR = 5
        const val SUBMIT_DEBOUNCE_WINDOW = 900L
        const val MAX_PHOTO_COUNT = 9
        const val COLLAPSED_REMARK_LINES = 3
        const val PRODUCT_MODE_NORMAL = REPAIR_MAINTAIN_TYPE_NORMAL
        const val PRODUCT_MODE_REPLACE = REPAIR_MAINTAIN_TYPE_CHANGE_PART
    }

    private data class RepairPhotoItem(
        val fileId: String,
        val localFile: File? = null,
    )
}
