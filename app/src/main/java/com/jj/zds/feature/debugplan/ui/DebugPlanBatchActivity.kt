package com.jj.zds.feature.debugplan.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.ConcatAdapter
import com.jj.core.permission.hasJiangPermission
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.recycler.page.JRecyclerPageHelper
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.ui.toolbar.JiangToolbarMenuItem
import com.jj.ui.widget.JStatusType
import com.jj.zds.data.dto.debugplan.DebugPlanDetailDto
import com.jj.zds.data.dto.debugplan.DebugPlanTaskItemDto
import com.jj.zds.databinding.ActivityDebugPlanBatchBinding
import com.jj.zds.databinding.ItemDebugPlanBatchBinding
import com.jj.zds.databinding.ItemDebugPlanBatchFooterBinding
import com.jj.zds.feature.photo.PhotoUploadFileProcessor
import com.jj.zds.feature.photo.PhotoUploadMode
import com.jj.zds.viewmodel.ZdsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class DebugPlanBatchActivity : JiangToolbarStateVmActivity<ActivityDebugPlanBatchBinding, ZdsViewModel>() {

    private var taskId: String = ""
    private var requireSubmitPhoto = false
    private var refreshOnResume = false
    private var finishingPlan = false
    private var submittingForcePass = false
    private var uploadingFinishPhoto = false
    private var finishPhotoUploadFailed = false
    private var finishPhotoFile: File? = null
    private var pendingPhotoFile: File? = null
    private var finishPhotoUploadMode = PhotoUploadMode.default()
    private val finishPhotoIds = mutableListOf<Long>()
    private lateinit var pageHelper: JRecyclerPageHelper<DebugPlanTaskItemDto, ItemDebugPlanBatchBinding>

    private val adapter = JBindingAdapter(
        inflate = ItemDebugPlanBatchBinding::inflate,
        onBind = ::bindBatch,
    )
    private val footerAdapter = JBindingAdapter<Unit, ItemDebugPlanBatchFooterBinding>(
        inflate = ItemDebugPlanBatchFooterBinding::inflate,
        onBind = ::bindFooter,
    )

    private val childPageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshDetail()
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchFinishPhotoCamera()
        } else {
            JToast.show("当前调试计划必须拍照后才能完工")
        }
    }

    private val takeFinishPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingPhotoFile
        if (success && file != null) {
            finishPhotoFile = file
            finishPhotoIds.clear()
            finishPhotoUploadFailed = false
            uploadingFinishPhoto = true
            renderFooter()
            val uploadFile = PhotoUploadFileProcessor.prepareUploadFile(
                source = file,
                mode = finishPhotoUploadMode,
                cacheDir = cacheDir,
                prefix = "debug_plan_finish",
            )
            viewModel.uploadDebugPlanFinishPhoto(uploadFile)
        } else {
            pendingPhotoFile = null
            renderFooter()
           // JToast.show("未完成拍照，计划暂不完工")
        }
    }

    override fun createViewBinding(inflater: LayoutInflater): ActivityDebugPlanBatchBinding {
        return ActivityDebugPlanBatchBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
        requireSubmitPhoto = intent.getBooleanExtra(EXTRA_REQUIRE_SUBMIT_PHOTO, false)
        if (taskId.isBlank()) {
            JToast.show("调试计划ID为空")
            finish()
            return
        }




        binding.recyclerBatches.setupLinear(ConcatAdapter(adapter, footerAdapter))
        binding.recyclerBatches.addDefaultDivider()
        pageHelper = JRecyclerPageHelper(
            recyclerView = binding.recyclerBatches,
            adapter = adapter,
            emptyView = binding.emptyView,
            refreshLayout = binding.refreshLayout,
        )
        binding.emptyView.setAction("刷新") {
            refreshDetail()
        }
        binding.refreshLayout.setOnRefreshListener {
            refreshDetail()
        }
        binding.btnCreateBatch.setOnClickListener {
            refreshOnResume = true
            childPageLauncher.launch(DebugPlanCreateBatchActivity.createIntent(this, taskId))
        }
        adapter.setOnItemClickListener { item, _ ->
            val taskItemId = item.id
            if (taskItemId.isNullOrBlank()) {
                JToast.show("批次ID为空")
                return@setOnItemClickListener
            }
            refreshOnResume = true
            childPageLauncher.launch(DebugTaskExecuteActivity.createIntent(this, taskItemId))
        }

        footerAdapter.setList(listOf(Unit))
        refreshDetail()
    }

    override fun onResume() {
        super.onResume()
        if (refreshOnResume) {
            refreshOnResume = false
            refreshDetail()
        }
    }

    override fun initPageObserver() {
        viewModel.debugPlanDetail.observe(this) { detail ->
            pageHelper.setRefreshing(false)
            renderDetail(detail)
        }
        viewModel.debugPlanFinishResult.observe(this) { result ->
            handlePlanFinishResult(result)
        }
        viewModel.debugPlanPhotoUploadResult.observe(this) { fileId ->
            handleFinishPhotoUploaded(fileId)
        }
    }

    override fun toolbarTitle(): CharSequence = "调试计划"

    override fun toolbarSubtitle(): CharSequence = "调试批次创建/执行"

    override fun toolbarMenuItems(): List<JiangToolbarMenuItem> {
        return listOf(JiangToolbarMenuItem(MENU_SYNC, "刷新"))
    }

    override fun onToolbarMenuClick(item: JiangToolbarMenuItem) {
        if (item.id == MENU_SYNC) {
            refreshDetail()
        }
    }

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        pageHelper.setRefreshing(false)
        if (uploadingFinishPhoto) {
            uploadingFinishPhoto = false
            finishPhotoUploadFailed = true
            pendingPhotoFile = null
            finishPhotoIds.clear()
            renderFooter()
            showPlanFinishError(message.ifBlank { "拍照凭证上传失败，请重新拍照" })
            return
        }
        if (finishingPlan) {
            finishingPlan = false
            if (code == CODE_FINISH_NEED_FORCE_PASS && !submittingForcePass) {
                showForceFinishConfirm(message)
                return
            }
            submittingForcePass = false
            showPlanFinishError(message)
            return
        }
        super.onUiError(code, message, throwable)
    }

    private fun refreshDetail() {
        pageHelper.setRefreshing(true)
        viewModel.loadDebugPlanDetail(taskId)
    }

    private fun renderDetail(detail: DebugPlanDetailDto) {
        binding.tvPlanCode.text = detail.code.orDash()
        binding.tvPlanMeta.text = buildString {
            append(detail.categoryName.orDash())
            detail.productName?.takeIf { it.isNotBlank() }?.let {
                append(" / ").append(it)
            }
            detail.specification?.takeIf { it.isNotBlank() }?.let {
                append(" / ").append(it)
            }
        }
        binding.tvBatchProgress.text = "${detail.finishCount.coerceAtLeast(0)} / ${detail.count.coerceAtLeast(0)}"
        binding.tvSampleProgress.text = "${detail.checkQty.coerceAtLeast(0)} / ${detail.sampleQty.coerceAtLeast(0)}"
        binding.tvNgProgress.text = "${detail.ngCount.coerceAtLeast(0)} / ${detail.rejectionQty.coerceAtLeast(0)}"
        binding.tvBatchCount.text = detail.list.size.toString()
        binding.progressBatch.progress = detail.progressPercent()
        pageHelper.showData(detail.list)
        renderFooter()
    }

    private fun bindFooter(
        itemBinding: ItemDebugPlanBatchFooterBinding,
        item: Unit,
        position: Int,
    ) {
        itemBinding.layoutFinishPhoto.visibility = if (requireSubmitPhoto) View.VISIBLE else View.GONE
        itemBinding.btnFinishPlan.setOnClickListener {
            confirmFinishPlan()
        }

        if (!requireSubmitPhoto) return

        val photoFile = finishPhotoFile
        val hasPhoto = photoFile != null
        itemBinding.ivFinishPhotoPreview.visibility = if (hasPhoto) View.VISIBLE else View.GONE
        itemBinding.btnDeleteFinishPhoto.visibility = if (hasPhoto) View.VISIBLE else View.GONE
        itemBinding.btnTakeFinishPhoto.text = if (hasPhoto) "重拍" else "拍照"

        if (photoFile != null) {
            itemBinding.ivFinishPhotoPreview.setImageURI(photoFile.toProviderUri())
        } else {
            itemBinding.ivFinishPhotoPreview.setImageDrawable(null)
        }

        itemBinding.tvFinishPhotoStatus.text = when {
            uploadingFinishPhoto -> "照片上传中，请稍候..."
            finishPhotoIds.isNotEmpty() -> "照片已上传，可以计划完工。"
            finishPhotoUploadFailed -> "照片上传失败，请重新拍照或删除后再拍。"
            hasPhoto -> "照片已拍摄，等待上传完成。"
            else -> "请先拍照上传，上传成功后才可以计划完工。"
        }
        itemBinding.groupFinishPhotoUploadMode.setOnCheckedChangeListener(null)
        itemBinding.groupFinishPhotoUploadMode.check(
            if (finishPhotoUploadMode == PhotoUploadMode.ORIGINAL) {
                itemBinding.rbFinishPhotoOriginal.id
            } else {
                itemBinding.rbFinishPhotoCompressed.id
            },
        )
        val canTakePhoto = !uploadingFinishPhoto && !finishingPlan
        itemBinding.groupFinishPhotoUploadMode.isEnabled = canTakePhoto
        itemBinding.rbFinishPhotoCompressed.isEnabled = canTakePhoto
        itemBinding.rbFinishPhotoOriginal.isEnabled = canTakePhoto
        itemBinding.groupFinishPhotoUploadMode.alpha = if (canTakePhoto) 1f else 0.6f
        itemBinding.groupFinishPhotoUploadMode.setOnCheckedChangeListener { _, checkedId ->
            finishPhotoUploadMode = if (checkedId == itemBinding.rbFinishPhotoOriginal.id) {
                PhotoUploadMode.ORIGINAL
            } else {
                PhotoUploadMode.COMPRESSED
            }
        }
        itemBinding.btnTakeFinishPhoto.setOnClickListener {
            requestCameraAndTakePhoto()
        }
        itemBinding.btnDeleteFinishPhoto.setOnClickListener {
            clearFinishPhoto()
        }
        itemBinding.ivFinishPhotoPreview.setOnClickListener {
            previewFinishPhoto()
        }
    }

    private fun bindBatch(
        itemBinding: ItemDebugPlanBatchBinding,
        item: DebugPlanTaskItemDto,
        position: Int,
    ) {
        val boxCount = item.boxLabelList.size
        val totalQty = item.boxLabelList.sumOf { it.quantity }
        itemBinding.tvBatchNo.text = item.batchNo?.takeIf { it.isNotBlank() } ?: "批次 ${position + 1}"
        itemBinding.tagStatus.setStatus(item.statusText(), item.statusTagType())
        itemBinding.tvBatchMeta.text = buildString {
            append("序号：").append(item.sort)
            item.createTime?.takeIf { it.isNotBlank() }?.let {
                append(" / 创建：").append(it)
            }
        }
        itemBinding.tvBoxCount.text = boxCount.toString()
        itemBinding.tvTotalQty.text = totalQty.toString()
        itemBinding.tvEnter.text = "进入执行"
    }

    private fun confirmFinishPlan(forcePass: Boolean = false) {
        if (requireSubmitPhoto && finishPhotoIds.isEmpty()) {
            JToast.show("请先完成拍照留痕")
            return
        }

        JConfirmDialog(this, this)
            .setTitle(if (forcePass) "强制完工" else "调试计划完工")
            .setMessage(
                if (forcePass) {
                    "当前调试计划还有未满足的完工条件，确认强制完工？"
                } else {
                    "确认当前调试计划已经全部调试完成？"
                },
            )
            .setConfirmText(if (forcePass) "强制完工" else "确认完工")
            .setOnConfirm {
                submitFinishPlan(forcePass)
            }
            .show()
    }

    private fun submitFinishPlan(forcePass: Boolean) {
        finishingPlan = true
        submittingForcePass = forcePass
        viewModel.finishDebugPlan(
            id = taskId,
            fileIdList = finishPhotoIds.toList(),
            forcePass = forcePass,
        )
    }

    private fun requestCameraAndTakePhoto() {
        if (hasJiangPermission(Manifest.permission.CAMERA)) {
            launchFinishPhotoCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchFinishPhotoCamera() {
        val file = createFinishPhotoFile()
        pendingPhotoFile = file
        takeFinishPhotoLauncher.launch(file.toProviderUri())
    }

    private fun clearFinishPhoto() {
        finishPhotoFile?.delete()
        pendingPhotoFile?.delete()
        finishPhotoFile = null
        pendingPhotoFile = null
        finishPhotoIds.clear()
        uploadingFinishPhoto = false
        finishPhotoUploadFailed = false
        renderFooter()
    }

    private fun previewFinishPhoto() {
        val file = finishPhotoFile ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.toProviderUri(), "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }
            .onFailure { JToast.show("当前设备没有可用的图片预览应用") }
    }

    private fun createFinishPhotoFile(): File {
        val dir = File(cacheDir, "debug_plan_finish_photo").apply {
            mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        return File(dir, "debug_plan_finish_${taskId}_${timestamp}.jpg")
    }

    private fun File.toProviderUri(): Uri {
        return FileProvider.getUriForFile(
            this@DebugPlanBatchActivity,
            "${packageName}.jiang.fileprovider",
            this,
        )
    }

    private fun handleFinishPhotoUploaded(fileId: Long) {
        uploadingFinishPhoto = false
        finishPhotoUploadFailed = false
        pendingPhotoFile = null
        finishPhotoIds.clear()
        finishPhotoIds.add(fileId)
        renderFooter()
        JToast.show("拍照凭证已上传")
    }

    private fun handlePlanFinishResult(result: Map<String, String>?) {
        finishingPlan = false
        submittingForcePass = false
        if (result.isNullOrEmpty()) {
            JToast.show("调试计划已完工")
            finish()
            return
        }

        showForceFinishConfirm(result)
    }

    private fun showForceFinishConfirm(result: Map<String, String>) {
        val message = result.entries.joinToString(separator = "\n") { (key, value) ->
            val label = key.takeIf { it.isNotBlank() } ?: "提示"
            val content = value.takeIf { it.isNotBlank() } ?: "-"
            "$label：$content"
        }
        JConfirmDialog(this, this)
            .setTitle("计划未满足完工条件")
            .setMessage(message)
            .setCancelText("暂不完工")
            .setConfirmText("强制完工")
            .setOnConfirm {
                submitFinishPlan(forcePass = true)
            }
            .show()
    }

    private fun showForceFinishConfirm(message: String) {
        JConfirmDialog(this, this)
            .setTitle("计划未满足完工条件")
            .setMessage(message.ifBlank { "当前计划未满足完工条件，是否继续完工？" })
            .setCancelText("暂不完工")
            .setConfirmText("确认完工")
            .setOnConfirm {
                submitFinishPlan(forcePass = true)
            }
            .show()
    }

    private fun showPlanFinishError(message: String) {
        JConfirmDialog(this, this)
            .setTitle("计划完工失败")
            .setMessage(message.ifBlank { "调试计划完工失败，请稍后重试" })
            .setShowCancelButton(false)
            .setConfirmText("知道了")
            .show()
    }

    private fun renderFooter() {
        if (footerAdapter.size() > 0) {
            footerAdapter.updateItem(0, Unit)
        }
    }

    private fun DebugPlanDetailDto.progressPercent(): Int {
        val total = count.coerceAtLeast(0)
        if (total == 0) return 0
        return (finishCount.coerceAtLeast(0) * 100 / total).coerceIn(0, 100)
    }

    private fun DebugPlanTaskItemDto.statusText(): String {
        return when (status) {
            1 -> "待调试"
            2 -> "调试中"
            3 -> "已完工"
            else -> "未知"
        }
    }

    private fun DebugPlanTaskItemDto.statusTagType(): JStatusType {
        return when (status) {
            1 -> JStatusType.WARNING
            2 -> JStatusType.INFO
            3 -> JStatusType.SUCCESS
            else -> JStatusType.NEUTRAL
        }
    }

    private fun String?.orDash(): String {
        return this?.takeIf { it.isNotBlank() } ?: "-"
    }

    companion object {
        private const val EXTRA_TASK_ID = "extra_task_id"
        private const val EXTRA_REQUIRE_SUBMIT_PHOTO = "extra_require_submit_photo"
        private const val MENU_SYNC = 1
        private const val CODE_FINISH_NEED_FORCE_PASS = 10011

        fun start(
            context: Context,
            taskId: String?,
            requireSubmitPhoto: Boolean = false,
        ) {
            if (taskId.isNullOrBlank()) {
                JToast.show("调试计划ID为空")
                return
            }
            context.startActivity(createIntent(context, taskId, requireSubmitPhoto))
        }

        fun createIntent(
            context: Context,
            taskId: String,
            requireSubmitPhoto: Boolean = false,
        ): Intent {
            return Intent(context, DebugPlanBatchActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_REQUIRE_SUBMIT_PHOTO, requireSubmitPhoto)
        }
    }
}
