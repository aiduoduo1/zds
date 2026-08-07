package com.jj.zds.feature.productreturn

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.data.dto.productreturn.ProductReturnScanResultDto
import com.jj.zds.databinding.ActivityProductReturnExecuteBinding
import com.jj.zds.databinding.ItemProductReturnRecordBinding

class ProductReturnExecuteActivity :
    JiangToolbarStateVmActivity<ActivityProductReturnExecuteBinding, ProductReturnExecuteViewModel>() {

    private val taskId by lazy { intent.getStringExtra(EXTRA_TASK_ID).orEmpty() }
    private val airportName by lazy { intent.getStringExtra(EXTRA_AIRPORT_NAME).orEmpty() }
    private val projectName by lazy { intent.getStringExtra(EXTRA_PROJECT_NAME).orEmpty() }
    private var scanInProgress = false
    private var saveInProgress = false
    private var operation = Operation.NONE
    private var scannedProducts = emptyList<ProductReturnScanResultDto>()
    private val adapter = JBindingAdapter(
        inflate = ItemProductReturnRecordBinding::inflate,
        onBind = ::bindRecord,
    )

    override fun createViewBinding(inflater: LayoutInflater) = ActivityProductReturnExecuteBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        if (taskId.isBlank()) finish()
        binding.recyclerProducts.setupLinear(adapter)
        binding.recyclerProducts.addDefaultDivider()
        adapter.setOnChildClickListener(R.id.btnRemoveProduct) { item, _ ->
            if (saveInProgress) return@setOnChildClickListener
            scannedProducts = scannedProducts.filterNot { it.matId == item.matId }
            renderProducts()
        }
        binding.inputReturnScan.setScanOnly(true)
        (binding.inputReturnScan.focusedInputView() as? EditText)?.isCursorVisible = true
        binding.inputReturnScan.onTextDebounce(delayMillis = 260L, listener = ::submitScan)
        binding.inputReturnScan.onInputDone(::submitScan)
        binding.inputReturnScan.onTextChanged { if (it.isBlank() && !scanInProgress) binding.inputReturnScan.clearState() }
        binding.btnSubmitReturn.setOnClickListener { confirmSave() }
        binding.btnFinishTask.setOnClickListener { confirmFinish() }
        renderProducts()
        focusScanner()
    }

    override fun initPageObserver() {
        viewModel.scanResult.observe(this) { product ->
            scanInProgress = false
            if (scannedProducts.any { it.matId == product.matId }) {
                showScanError("该成品已在本次待退库集合中")
            } else {
                scannedProducts = listOf(product) + scannedProducts
                binding.inputReturnScan.clearText().clearState()
                renderProducts()
                JToast.show("扫描成功，已加入待退库集合")
                focusScanner()
            }
        }
        viewModel.saved.observe(this) {
            saveInProgress = false
            scannedProducts = emptyList()
            binding.inputReturnScan.clearText().clearState()
            renderProducts()
            JToast.show("退库提交成功")
            focusScanner()
        }
        viewModel.finished.observe(this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun toolbarTitle(): CharSequence = airportName.ifBlank { "机场" }
    override fun toolbarSubtitle(): CharSequence = projectName.ifBlank { "项目" }
    override fun enableStateLayout() = false
    override fun errorMode() = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        super.onUiError(code, message, throwable)
        scanInProgress = false
        saveInProgress = false
        when (operation) {
            Operation.SCAN -> showScanError(message)
            Operation.SAVE -> showSaveError(message)
            else -> if (message.isNotBlank()) JToast.show(message)
        }
        operation = Operation.NONE
        renderProducts()
    }

    private fun submitScan(rawCode: String) {
        val code = rawCode.trim()
        if (code.isBlank() || scanInProgress || saveInProgress) return
        scanInProgress = true
        operation = Operation.SCAN
        binding.inputReturnScan.verifying("正在校验退库资格…")
        renderProducts()
        viewModel.scan(taskId, code)
    }

    private fun confirmSave() {
        val ids = scannedProducts.mapNotNull { it.matId?.takeIf(String::isNotBlank) }
        if (ids.isEmpty() || saveInProgress) return
        JConfirmDialog(this, this)
            .setTitle("确认退库？")
            .setMessage("本次将退库 ${ids.size} 台成品，提交后不可在 PDA 撤回。")
            .setConfirmText("确认退库")
            .setOnConfirm {
                saveInProgress = true
                operation = Operation.SAVE
                renderProducts()
                viewModel.save(taskId, ids)
            }
            .show()
    }

    private fun confirmFinish() {
        val message = if (scannedProducts.isEmpty()) "结束后将不能继续扫码退库。" else "当前有 ${scannedProducts.size} 台未提交，结束后这些暂存项不会退库。"
        JConfirmDialog(this, this)
            .setTitle("结束退库任务？")
            .setMessage(message)
            .setConfirmText("确认结束")
            .setOnConfirm { operation = Operation.FINISH; viewModel.finish(taskId) }
            .show()
    }

    private fun showScanError(message: String) {
        binding.inputReturnScan.showError(message).selectAll()
        renderProducts()
        JConfirmDialog(this, this)
            .setTitle("扫描失败")
            .setMessage(message)
            .setConfirmText("知道了")
            .setShowCancelButton(false)
            .setOnDismiss(::focusScanner)
            .show()
    }

    private fun showSaveError(message: String) {
        JConfirmDialog(this, this)
            .setTitle("退库失败")
            .setMessage(
                message.ifBlank { "退库集合中存在不符合退库条件的成品，请检查后重试。" },
            )
            .setConfirmText("知道了")
            .setShowCancelButton(false)
            .show()
    }

    private fun renderProducts() {
        adapter.submitListWithDiff(scannedProducts)
        binding.emptyProducts.visibility = if (scannedProducts.isEmpty()) View.VISIBLE else View.GONE
        binding.tvReturnSummary.text = "本次待退库 ${scannedProducts.size} 台"
        val operationEnabled = !scanInProgress && !saveInProgress
        binding.btnSubmitReturn.isEnabled = scannedProducts.isNotEmpty() && operationEnabled
        binding.btnFinishTask.isEnabled = operationEnabled
        binding.inputReturnScan.setInputEnabled(operationEnabled)
        if (operationEnabled) binding.inputReturnScan.setScanOnly(true)
    }

    private fun bindRecord(binding: ItemProductReturnRecordBinding, item: ProductReturnScanResultDto, position: Int) {
        binding.tvProductIndex.text = (position + 1).toString()
        binding.tvProductCode.text = item.materialCode.orDash()
        binding.tvProductName.text = "产品：${item.productName.orDash(item.categoryName)}"
        binding.tvMaterialNo.text = "物料号：${item.categoryName.orDash()}"
        binding.tvOutTask.text = "原出库单：${item.outTaskCode.orDash()}"
        binding.tvAirport.text = "出货机场：${item.fromAirPortName.orDash()}"
    }

    private fun focusScanner() = binding.inputReturnScan.post { binding.inputReturnScan.requestInputFocus(showKeyboard = false) }
    private fun String?.orDash(vararg fallback: String?) = sequenceOf(this, *fallback).firstOrNull { !it.isNullOrBlank() } ?: "-"
    private enum class Operation { NONE, SCAN, SAVE, FINISH }

    companion object {
        private const val EXTRA_TASK_ID = "extra_return_task_id"
        private const val EXTRA_AIRPORT_NAME = "extra_return_airport_name"
        private const val EXTRA_PROJECT_NAME = "extra_return_project_name"
        fun intent(context: Context, taskId: String, airportName: String, projectName: String) = Intent(context, ProductReturnExecuteActivity::class.java)
            .putExtra(EXTRA_TASK_ID, taskId)
            .putExtra(EXTRA_AIRPORT_NAME, airportName)
            .putExtra(EXTRA_PROJECT_NAME, projectName)
    }
}
