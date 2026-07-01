package com.jj.zds.feature.singledebug.ui

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.text.InputFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ConcatAdapter
import coil.load
import com.jj.core.permission.hasJiangPermission
import com.jj.storage.JiangStorage
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JInputDialog
import com.jj.ui.dialog.JSingleChoiceDialog
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.recycler.multitype.JMultiBindingAdapter
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.data.dto.debugplan.DebugCheckGroupDto
import com.jj.zds.data.dto.debugplan.DebugCheckItemDto
import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import com.jj.zds.data.dto.debugplan.DebugSnScanValidDto
import com.jj.zds.data.local.ZdsStorageKeys
import com.jj.zds.databinding.ActivitySingleDebugBinding
import com.jj.zds.databinding.ItemSingleDebugFooterBinding
import com.jj.zds.databinding.ItemSingleDebugHeaderBinding
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
import com.jj.zds.feature.debugplan.ui.DebugTaskCollectType
import com.jj.zds.feature.debugplan.ui.DebugTaskInputRules
import com.jj.zds.feature.debugplan.ui.DebugTaskSubmitRules
import com.jj.zds.feature.photo.PhotoUploadFileProcessor
import com.jj.zds.feature.photo.PhotoUploadMode
import com.jj.zds.feature.repair.data.buildRepairFileUrl
import com.jj.zds.viewmodel.ZdsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class SingleDebugActivity : JiangToolbarStateVmActivity<ActivitySingleDebugBinding, ZdsViewModel>() {

    private var qrCode: String = ""
    private var lastSubmitCode: String = ""
    private var lastSubmitAt: Long = 0L
    private var scanInProgress = false
    private var activeOperation = ActiveOperation.NONE
    private var currentSn: DebugSnScanValidDto? = null
    private var currentCheckGroup: DebugCheckGroupDto? = null
    private var pendingPhotoCheckId: String? = null
    private var pendingPhotoFile: File? = null
    private var clearingSnForm = false
    private val checks = mutableListOf<DebugTaskCheckRow>()

    private val headerAdapter = JBindingAdapter<Unit, ItemSingleDebugHeaderBinding>(
        inflate = ItemSingleDebugHeaderBinding::inflate,
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
    private val footerAdapter = JBindingAdapter<Unit, ItemSingleDebugFooterBinding>(
        inflate = ItemSingleDebugFooterBinding::inflate,
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
                prefix = "single_debug_check",
            )
            viewModel.uploadDebugTaskCheckPhoto(checkId, uploadFile)
        } else {
            pendingPhotoCheckId = null
            pendingPhotoFile = null
        }
    }

    override fun createViewBinding(inflater: LayoutInflater): ActivitySingleDebugBinding {
        return ActivitySingleDebugBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
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
        bindScanInput()
        bindImeInsets()
        headerAdapter.setList(listOf(Unit))
        footerAdapter.setList(listOf(Unit))
        renderChecks()
        focusScanInput(selectAll = false)
    }

    override fun initPageObserver() {
        viewModel.singleDebugSnScanValidResult.observe(this) { result ->
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
        viewModel.debugOperationDone.observe(this) {
            when (activeOperation) {
                ActiveOperation.SAVE_DRAFT -> handleSaveDone()
                ActiveOperation.SUBMIT -> handleSubmitDone()
                else -> Unit
            }
        }
    }

    override fun toolbarTitle(): CharSequence = "单个调试"

    override fun toolbarSubtitle(): CharSequence = "扫描 SN 后开始调试"

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        val wasScanInProgress = scanInProgress
        scanInProgress = false
        pendingPhotoCheckId?.let { checkId ->
            updatePhotoUploading(checkId, uploading = false)
            pendingPhotoCheckId = null
            pendingPhotoFile = null
        }
        activeOperation = ActiveOperation.NONE
        footerAdapter.updateItem(0, Unit)
        if (wasScanInProgress) {
            showScanError(message)
            return
        }
        focusScanInput(selectAll = true)
        super.onUiError(code, message, throwable)
    }

    private fun bindHeader(
        itemBinding: ItemSingleDebugHeaderBinding,
        item: Unit,
        position: Int,
    ) {
        val group = currentCheckGroup
        if (group == null) {
            itemBinding.tvFormName.text = "请扫描 SN"
            itemBinding.layoutSnInfo.visibility = View.GONE
        } else {
            val versionText = group.showVersion?.takeIf { it.isNotBlank() } ?: "V${group.version}"
            itemBinding.tvFormName.text = "${group.name.orDash()} $versionText"
            val sn = currentSn?.snResult
            itemBinding.layoutSnInfo.visibility = View.VISIBLE
            itemBinding.tvSn.text = "SN：${sn?.sn.orDash()}"
            itemBinding.tvSnMeta.text = "物料编号：${sn?.categoryName.orDash()}    数量：${sn?.qty ?: 0}"
        }
    }

    private fun bindScanInput() {
        binding.inputSn.setText(qrCode)
        binding.inputSn.onTextChanged {
            qrCode = it
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

    private fun bindFooter(
        itemBinding: ItemSingleDebugFooterBinding,
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
        val canOperate = currentSn != null && activeOperation == ActiveOperation.NONE
        itemBinding.btnSave.text = if (activeOperation == ActiveOperation.SAVE_DRAFT) "保存中..." else "保存"
        itemBinding.btnSave.isEnabled = canOperate
        itemBinding.btnSave.alpha = if (itemBinding.btnSave.isEnabled) 1f else 0.6f
        itemBinding.btnSave.setOnClickListener {
            saveDraft()
        }
        itemBinding.btnSubmit.text = if (activeOperation == ActiveOperation.SUBMIT) "提交中..." else "提交"
        itemBinding.btnSubmit.isEnabled = canOperate
        itemBinding.btnSubmit.alpha = if (itemBinding.btnSubmit.isEnabled) 1f else 0.6f
        itemBinding.btnSubmit.setOnClickListener {
            submitChecks()
        }
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
        viewModel.scanValidSingleDebugSn(code)
    }

    private fun renderSnScanResult(result: DebugSnScanValidDto) {
        currentSn = result
        qrCode = result.qrCode ?: qrCode
        currentCheckGroup = result.checkGrp ?: result.checkGroupDto
        checks.clear()
        checks += currentCheckGroup?.toRows().orEmpty()
        headerAdapter.updateItem(0, Unit)
        currentCheckGroup?.let { group ->
            val versionText = group.showVersion?.takeIf { it.isNotBlank() } ?: "V${group.version}"
            binding.inputSn.showSuccess("${group.name.orDash()} $versionText")
        }
        renderChecks()
        footerAdapter.updateItem(0, Unit)
        if (checks.isEmpty()) {
            JToast.show("该 SN 未返回检查项")
        }
    }

    private fun submitChecks() {
        val group = currentCheckGroup
        if (currentSn == null || group == null) {
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
        submitSingleDebug(ngPhenomena = null, isDraft = false)
    }

    private fun saveDraft() {
        if (currentSn == null || currentCheckGroup == null) {
            JToast.show("请先扫描 SN")
            focusScanInput(selectAll = false)
            return
        }
        submitSingleDebug(ngPhenomena = null, isDraft = true)
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
                } else {
                    submitSingleDebug(value, isDraft = false)
                }
            }
            .show()
    }

    private fun submitSingleDebug(ngPhenomena: String?, isDraft: Boolean) {
        val group = currentCheckGroup ?: return
        activeOperation = if (isDraft) ActiveOperation.SAVE_DRAFT else ActiveOperation.SUBMIT
        footerAdapter.updateItem(0, Unit)
        viewModel.submitSingleDebugCheck(
            qrCode = qrCode,
            ngPhenomena = ngPhenomena,
            checkGroupDto = group.withRowResults(checks),
            isDraft = isDraft,
        )
    }

    private fun handleSaveDone() {
        activeOperation = ActiveOperation.NONE
        footerAdapter.updateItem(0, Unit)
        JToast.show("草稿已保存")
    }

    private fun handleSubmitDone() {
        resetSnForm(clearInput = true)
        focusScanInput(selectAll = false)
        JToast.show("单个调试已提交")
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
        qrCode = ""
        lastSubmitCode = ""
        lastSubmitAt = 0L
        scanInProgress = false
        binding.inputSn.clearState()
        renderChecks()
        headerAdapter.updateItem(0, Unit)
        footerAdapter.updateItem(0, Unit)
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
            JToast.show("检查照片最多 9 张")
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

    private fun renderChecks() {
        checkAdapter.submitListWithDiff(checks.toList())
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

    private fun scrollCheckIntoView(id: String) {
        val position = checks.indexOfFirst { it.checkId == id }
        if (position < 0) return
        val adapterPosition = position + HEADER_ITEM_COUNT
        binding.recyclerChecks.post { requestCheckVisible(adapterPosition) }
        binding.recyclerChecks.postDelayed({ requestCheckVisible(adapterPosition) }, 180L)
    }

    private fun scrollToFirstPending() {
        val position = checks.indexOfFirst {
            it.result == DebugTaskCheckResult.PENDING && !it.dto.isSkipable
        }
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
        focusScanInput(selectAll = true)
    }

    private fun focusScanInput(selectAll: Boolean) {
        binding.inputSn.post {
            binding.inputSn.requestInputFocus(showKeyboard = false)
            if (selectAll) {
                binding.inputSn.selectAll()
            }
        }
    }

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

    private fun createCheckPhotoFile(checkId: String): File {
        val dir = File(cacheDir, "single_debug_check_photo").apply {
            mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        return File(dir, "single_debug_${checkId}_$timestamp.jpg")
    }

    private fun File.toProviderUri(): Uri {
        return FileProvider.getUriForFile(
            this@SingleDebugActivity,
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
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val closeView = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END)
            gravity = Gravity.CENTER
            text = "×"
            textSize = 30f
            setTextColor(Color.WHITE)
        }
        popupRoot.addView(imageView)
        popupRoot.addView(closeView)
        val popup = PopupWindow(
            popupRoot,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true,
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
        }
        closeView.setOnClickListener { popup.dismiss() }
        val source = photo.localFile?.takeIf { it.exists() } ?: buildRepairFileUrl(photo.fileId, currentToken())
        imageView.load(source)
        popup.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
    }

    private fun currentToken(): String {
        return JiangStorage.kv.getString(ZdsStorageKeys.MES_UP_TOKEN, "").orEmpty()
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun String?.orDash(): String {
        return this?.takeIf { it.isNotBlank() } ?: "-"
    }

    private enum class ActiveOperation {
        NONE,
        WAITING_NG,
        SAVE_DRAFT,
        SUBMIT,
    }

    private companion object {
        const val HEADER_ITEM_COUNT = 1
        const val PAYLOAD_CHECK_CHANGED = "singleDebugCheckChanged"
        const val SUBMIT_DEBOUNCE_WINDOW = 800L
        const val MAX_PHOTO_COUNT = 9
    }
}
