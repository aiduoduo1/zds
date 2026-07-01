package com.jj.zds.feature.debugplan.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.data.dto.debugplan.DebugBoxScanValidDto
import com.jj.zds.databinding.ActivityDebugPlanCreateBatchBinding
import com.jj.zds.databinding.ItemDebugPlanCreateBoxBinding
import com.jj.zds.viewmodel.ZdsViewModel

class DebugPlanCreateBatchActivity : JiangToolbarStateVmActivity<ActivityDebugPlanCreateBatchBinding, ZdsViewModel>() {

    private var taskId: String = ""
    private var planMaterialCode: String = ""
    private var pendingScanCode: String = ""
    private var lastSubmitCode: String = ""
    private var lastSubmitAt: Long = 0L
    private var scanInProgress = false
    private var submitInProgress = false
    private val boxes = mutableListOf<PendingBox>()

    private val adapter = JBindingAdapter(
        inflate = ItemDebugPlanCreateBoxBinding::inflate,
        onBind = ::bindBox,
    )

    override fun createViewBinding(inflater: LayoutInflater): ActivityDebugPlanCreateBatchBinding {
        return ActivityDebugPlanCreateBatchBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
        planMaterialCode = intent.getStringExtra(EXTRA_PLAN_MATERIAL_CODE).orEmpty()
        if (taskId.isBlank()) {
            JToast.show("调试计划ID为空")
            finish()
            return
        }

        binding.recyclerBoxes.setupLinear(adapter)
        binding.recyclerBoxes.addDefaultDivider()
        adapter.setOnChildClickListener(R.id.btnRemoveBox) { item, _ ->
            boxes.remove(item)
            renderBoxes()
            focusScanInput(selectAll = false)
        }

        binding.inputBoxScan.onTextChanged {
            if (it != lastSubmitCode) {
                lastSubmitCode = ""
            }
            binding.inputBoxScan.clearState()
        }
        binding.inputBoxScan.onTextDebounce(delayMillis = 260L) {
            submitScan(it)
        }
        binding.inputBoxScan.onInputDone {
            submitScan(it)
        }
        binding.btnSubmitBatch.setOnClickListener {
            submitBatch()
        }

        renderBoxes()
        focusScanInput(selectAll = false)
    }

    override fun initPageObserver() {
        viewModel.debugBoxScanValidResult.observe(this) { result ->
            scanInProgress = false
            addBox(result)
        }
        viewModel.debugOperationDone.observe(this) {
            submitInProgress = false
            JToast.show("批次创建成功")
            finish()
        }
    }

    override fun toolbarTitle(): CharSequence = "创建批次"

    override fun toolbarSubtitle(): CharSequence = "扫描料箱码"

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        super.onUiError(code, message, throwable)
        scanInProgress = false
        submitInProgress = false
        renderBoxes()
        binding.inputBoxScan.showError(message).selectAll()
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

    /**
     * 料箱扫码入口：过滤空码、重复扫码和短时间重复触发，再交给后端解析二维码。
     */
    private fun submitScan(rawCode: String) {
        val code = rawCode.trim()
        val now = SystemClock.elapsedRealtime()
        if (code.isBlank()) return
        if (scanInProgress) return
        if (code == lastSubmitCode && now - lastSubmitAt < SUBMIT_DEBOUNCE_WINDOW) return
        if (DebugPlanBoxMaterialValidator.isDuplicateBox(boxes.flatMap { listOf(it.boxNo, it.code) }, code, null)) {
            showScanErrorDialog("箱码已在批次列表中")
            return
        }

        pendingScanCode = code
        lastSubmitCode = code
        lastSubmitAt = now
        scanInProgress = true
        binding.inputBoxScan.verifying("料箱校验中...")
        viewModel.scanValidDebugBox(taskId, code)
    }

    /**
     * 后端解析成功后加入本地待提交队列；真正创建批次前都只维护内存列表。
     */
    private fun addBox(result: DebugBoxScanValidDto) {
        val code = pendingScanCode
        if (code.isBlank()) return
        val boxNo = result.box.orEmpty()
        if (!DebugPlanBoxMaterialValidator.isMaterialMatched(result.categoryName, planMaterialCode)) {
            pendingScanCode = ""
            val message = DebugPlanBoxMaterialValidator.mismatchMessage(
                boxNo = boxNo.ifBlank { code },
                planMaterialCode = planMaterialCode,
            )
            showScanErrorDialog(message)
            return
        }
        if (DebugPlanBoxMaterialValidator.isDuplicateBox(boxes.flatMap { listOf(it.boxNo, it.code) }, code, boxNo)) {
            pendingScanCode = ""
            showScanErrorDialog("箱码已在批次列表中")
            return
        }

        boxes.add(
            PendingBox(
                code = code,
                boxNo = boxNo.ifBlank { code },
                data = result,
            ),
        )
        pendingScanCode = ""
        binding.inputBoxScan.clearText()
        binding.inputBoxScan.showSuccess("校验通过，已加入待提交列表")
        renderBoxes()
        focusScanInput(selectAll = false)
    }

    /**
     * 创建批次提交入口，只提交扫码原始 code 列表，料箱详情仅用于页面确认。
     */
    private fun submitBatch() {
        if (submitInProgress) return
        if (boxes.isEmpty()) {
            JToast.show("请先扫描料箱码")
            focusScanInput(selectAll = false)
            return
        }
        submitInProgress = true
        renderBoxes()
        viewModel.bindDebugBoxes(taskId, boxes.map { it.code })
    }

    /**
     * 统一刷新待提交列表、空态、数量合计和按钮状态，避免局部状态漏刷新。
     */
    private fun renderBoxes() {
        adapter.submitListWithDiff(boxes.toList())
        binding.emptyView.visibility = if (boxes.isEmpty()) View.VISIBLE else View.GONE
        binding.tvBoxCount.text = boxes.size.toString()
        binding.tvQtyCount.text = boxes.sumOf { it.data.qty }.toString()
        binding.btnSubmitBatch.isEnabled = boxes.isNotEmpty() && !submitInProgress
        binding.btnSubmitBatch.alpha = if (boxes.isNotEmpty() && !submitInProgress) 1f else 0.6f
        binding.btnSubmitBatch.text = if (submitInProgress) "提交中..." else "提交创建批次"
    }

    private fun bindBox(
        itemBinding: ItemDebugPlanCreateBoxBinding,
        item: PendingBox,
        position: Int,
    ) {
        val data = item.data
        itemBinding.tvBoxNo.text = item.boxNo
        itemBinding.tvBoxMeta.text = buildString {
            append(data.categoryName.orDash())
            data.name?.takeIf { it.isNotBlank() }?.let {
                append(" / ").append(it)
            }
            data.lot?.takeIf { it.isNotBlank() }?.let {
                append(" / 批次：").append(it)
            }
        }
        itemBinding.tvQty.text = data.qty.toString()
        itemBinding.tvSnRange.text = listOfNotNull(data.startSn, data.endSn).joinToString(" - ").ifBlank { "-" }
        itemBinding.tvIndex.text = (position + 1).toString()
    }

    private fun focusScanInput(selectAll: Boolean) {
        binding.inputBoxScan.post {
            binding.inputBoxScan.requestInputFocus(showKeyboard = false)
            if (selectAll) {
                binding.inputBoxScan.selectAll()
            }
        }
    }

    private fun showScanErrorDialog(message: String) {
        binding.inputBoxScan.showError(message).selectAll()
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

    private fun String?.orDash(): String {
        return this?.takeIf { it.isNotBlank() } ?: "-"
    }

    private data class PendingBox(
        val code: String,
        val boxNo: String,
        val data: DebugBoxScanValidDto,
    )

    companion object {
        private const val EXTRA_TASK_ID = "extra_task_id"
        private const val EXTRA_PLAN_MATERIAL_CODE = "extra_plan_material_code"
        private const val SUBMIT_DEBOUNCE_WINDOW = 800L

        fun start(context: Context, taskId: String?) {
            if (taskId.isNullOrBlank()) {
                JToast.show("调试计划ID为空")
                return
            }
            context.startActivity(createIntent(context, taskId))
        }

        fun createIntent(
            context: Context,
            taskId: String,
            planMaterialCode: String = "",
        ): Intent {
            return Intent(context, DebugPlanCreateBatchActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_PLAN_MATERIAL_CODE, planMaterialCode)
        }
    }
}
