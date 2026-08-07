package com.jj.zds.feature.productstockout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.dialog.JInputDialog
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.data.dto.productstockout.ProductStockOutDetailDto
import com.jj.zds.data.dto.productstockout.ProductStockOutRecordDto
import com.jj.zds.databinding.ActivityProductStockOutExecuteBinding
import com.jj.zds.databinding.ItemProductStockOutRecordBinding

class ProductStockOutExecuteActivity :
    JiangToolbarStateVmActivity<ActivityProductStockOutExecuteBinding, ProductStockOutExecuteViewModel>() {

    private val adapter = JBindingAdapter(
        inflate = ItemProductStockOutRecordBinding::inflate,
        onBind = ::bindRecord,
    )
    private val taskDetailId: String by lazy { intent.getStringExtra(EXTRA_DETAIL_ID).orEmpty() }
    private val productName: String by lazy { intent.getStringExtra(EXTRA_PRODUCT_NAME).orEmpty() }
    private val productCode: String by lazy { intent.getStringExtra(EXTRA_PRODUCT_CODE).orEmpty() }
    private var scanInProgress = false
    private var activeOperation = Operation.NONE

    override fun createViewBinding(inflater: LayoutInflater) = ActivityProductStockOutExecuteBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerRecords.setupLinear(adapter)
        binding.recyclerRecords.addDefaultDivider()
        adapter.setOnChildClickListener(R.id.btnDeleteRecord) { record, _ -> confirmDelete(record) }
        binding.inputStockOutScan.setScanOnly(true)
        (binding.inputStockOutScan.focusedInputView() as? EditText)?.isCursorVisible = true
        binding.inputStockOutScan.onTextDebounce(delayMillis = 260L, listener = ::submitScan)
        binding.inputStockOutScan.onInputDone(::submitScan)
        binding.inputStockOutScan.onTextChanged { text ->
            if (text.isBlank() && !scanInProgress) binding.inputStockOutScan.clearState()
        }
        binding.btnFinishChildTask.setOnClickListener { confirmFinishChildTask() }
        focusScanner()
    }

    override fun initPageObserver() {
        viewModel.detail.observe(this, ::renderDetail)
        viewModel.scanResult.observe(this) {
            scanInProgress = false
            activeOperation = Operation.LOAD_DETAIL
            binding.inputStockOutScan.clearText().clearState()
            focusScanner()
            viewModel.loadDetail(taskDetailId)
        }
        viewModel.recordDeleted.observe(this) {
            activeOperation = Operation.LOAD_DETAIL
            viewModel.loadDetail(taskDetailId)
        }
        viewModel.childTaskFinished.observe(this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun initData() {
        if (taskDetailId.isBlank()) finish() else viewModel.loadDetail(taskDetailId)
    }

    override fun toolbarTitle(): CharSequence = productName.ifBlank { "物料名称" }

    override fun toolbarSubtitle(): CharSequence = productCode.ifBlank { "物料编码" }

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        super.onUiError(code, message, throwable)
        scanInProgress = false
        if (activeOperation == Operation.SCAN) {
            binding.inputStockOutScan.showError(message).selectAll()
            JConfirmDialog(this, this)
                .setTitle("出库失败")
                .setMessage(message)
                .setConfirmText("知道了")
                .setShowCancelButton(false)
                .setOnDismiss(::focusScanner)
                .show()
        } else if (activeOperation == Operation.FINISH_CHILD && code == CODE_FORCE_FINISH_REQUIRED) {
            showForceFinishDialog(message)
        } else if (message.isNotBlank()) {
            JToast.show(message)
        }
        renderDetail(viewModel.detail.value)
        activeOperation = Operation.NONE
    }

    private fun submitScan(rawCode: String) {
        val qrCode = rawCode.trim()
        if (qrCode.isBlank() || scanInProgress) return
        scanInProgress = true
        activeOperation = Operation.SCAN
        binding.inputStockOutScan.verifying("出库提交中...")
        renderDetail(viewModel.detail.value)
        viewModel.submitScan(taskDetailId, qrCode)
    }

    private fun renderDetail(detail: ProductStockOutDetailDto?) {
        val records = detail?.resList.orEmpty()
        adapter.submitListWithDiff(records)
        binding.emptyRecords.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        binding.tvExecuteSummary.text = "需求 ${detail?.reqQuantity ?: 0} 台 · 已出库 ${detail?.deliveryQuantity ?: 0} 台"
        val completed = detail?.status == STATUS_FINISHED || detail?.statusLabel == "已完成"
        binding.inputStockOutScan.visibility = if (completed) View.GONE else View.VISIBLE
        binding.btnFinishChildTask.visibility = if (completed) View.GONE else View.VISIBLE
        binding.inputStockOutScan.setInputEnabled(!scanInProgress && !completed)
        binding.btnFinishChildTask.isEnabled = !scanInProgress && !completed
        binding.btnFinishChildTask.alpha = if (binding.btnFinishChildTask.isEnabled) 1f else 0.6f
        if (!scanInProgress && !completed) binding.inputStockOutScan.setScanOnly(true)
    }

    private fun bindRecord(
        itemBinding: ItemProductStockOutRecordBinding,
        item: ProductStockOutRecordDto,
        position: Int,
    ) {
        itemBinding.tvRecordIndex.text = (position + 1).toString()
        itemBinding.tvRecordCode.text = item.materialCode.orDash()
        itemBinding.tvRecordQuantity.text = "数量：${item.quantity ?: 0}"
        itemBinding.tvRecordOperator.text = "出库人：${item.createByName.orDash()}"
        itemBinding.tvRecordTime.text = "出库时间：${item.createTime.orDash()}"
        val completed = viewModel.detail.value?.status == STATUS_FINISHED ||
            viewModel.detail.value?.statusLabel == "已完成"
        itemBinding.btnDeleteRecord.visibility = if (completed) View.GONE else View.VISIBLE
    }

    private fun confirmDelete(record: ProductStockOutRecordDto) {
        val recordId = record.id?.takeIf { it.isNotBlank() } ?: return
        JConfirmDialog(this, this)
            .setTitle("撤回该出库记录？")
            .setMessage("${record.materialCode.orDash()} 将恢复为未出库状态。")
            .setConfirmText("确认撤回")
            .setOnConfirm {
                activeOperation = Operation.DELETE
                viewModel.deleteRecord(recordId)
            }
            .show()
    }

    private fun confirmFinishChildTask() {
        JConfirmDialog(this, this)
            .setTitle("结束子任务？")
            .setMessage("结束后将无法继续扫码出库。")
            .setConfirmText("确认结束")
            .setOnConfirm {
                activeOperation = Operation.FINISH_CHILD
                viewModel.finishChildTask(taskDetailId, force = false)
            }
            .show()
    }

    private fun showForceFinishDialog(message: String) {
        JInputDialog(this, this)
            .setTitle("强制结束任务")
            .setInputTitle("结束备注")
            .setHint("请输入强制结束原因（最多20字）")
            .setOnConfirm { rawRemark ->
                val remark = rawRemark.trim()
                when {
                    remark.isBlank() -> JToast.show("请输入强制结束原因")
                    remark.length > FORCE_FINISH_REMARK_MAX_LENGTH -> JToast.show("备注最多输入 20 个字符")
                    else -> {
                        activeOperation = Operation.FORCE_FINISH_CHILD
                        viewModel.finishChildTask(taskDetailId, force = true, remark = remark)
                    }
                }
            }
            .show()
    }

    private fun focusScanner() {
        binding.inputStockOutScan.post { binding.inputStockOutScan.requestInputFocus(showKeyboard = false) }
    }

    private fun String?.orDash(fallback: String = "-") = this?.takeIf { it.isNotBlank() } ?: fallback

    private enum class Operation { NONE, LOAD_DETAIL, SCAN, DELETE, FINISH_CHILD, FORCE_FINISH_CHILD }

    companion object {
        private const val STATUS_FINISHED = 2
        private const val CODE_FORCE_FINISH_REQUIRED = 10086
        private const val FORCE_FINISH_REMARK_MAX_LENGTH = 20
        private const val EXTRA_DETAIL_ID = "extra_detail_id"
        private const val EXTRA_PRODUCT_NAME = "extra_product_name"
        private const val EXTRA_PRODUCT_CODE = "extra_product_code"

        fun intent(
            context: Context,
            taskDetailId: String,
            productName: String,
            productCode: String,
        ): Intent {
            return Intent(context, ProductStockOutExecuteActivity::class.java)
                .putExtra(EXTRA_DETAIL_ID, taskDetailId)
                .putExtra(EXTRA_PRODUCT_NAME, productName)
                .putExtra(EXTRA_PRODUCT_CODE, productCode)
        }
    }
}
