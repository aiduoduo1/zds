package com.jj.zds.feature.productstockin

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
import com.jj.zds.databinding.ActivityProductStockInBinding
import com.jj.zds.databinding.ItemProductStockInBinding
import com.jj.zds.data.dto.productstockin.ProductStockInRecordDto

class ProductStockInActivity :
    JiangToolbarStateVmActivity<ActivityProductStockInBinding, ProductStockInViewModel>() {

    private var scanInProgress = false

    private val adapter = JBindingAdapter(
        inflate = ItemProductStockInBinding::inflate,
        onBind = ::bindRecord,
    )

    override fun createViewBinding(inflater: LayoutInflater) =
        ActivityProductStockInBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerProducts.setupLinear(adapter)
        binding.recyclerProducts.addDefaultDivider()
        binding.inputStockInScan.setScanOnly(true)
        (binding.inputStockInScan.focusedInputView() as? EditText)?.isCursorVisible = true
        binding.inputStockInScan.onTextDebounce(delayMillis = 260L, listener = ::submitScan)
        binding.inputStockInScan.onInputDone(::submitScan)
        binding.inputStockInScan.onTextChanged { text ->
            if (text.isBlank() && !scanInProgress) binding.inputStockInScan.clearState()
        }
        renderRecords(viewModel.stockInRecords.value.orEmpty())
        focusScanner()
    }

    override fun initPageObserver() {
        viewModel.stockInRecords.observe(this) { records ->
            scanInProgress = false
            binding.inputStockInScan.clearText().clearState()
            renderRecords(records)
            focusScanner()
        }
    }

    override fun toolbarTitle(): CharSequence = "成品入库"

    override fun toolbarSubtitle(): CharSequence = "扫描流转单或成品码，自动入库"

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        scanInProgress = false
        binding.inputStockInScan.showError(message).selectAll()
        renderRecords(viewModel.stockInRecords.value.orEmpty())
        JConfirmDialog(this, this)
            .setTitle("入库失败")
            .setMessage(message)
            .setConfirmText("知道了")
            .setShowCancelButton(false)
            .setOnDismiss(::focusScanner)
            .show()
    }

    private fun submitScan(rawCode: String) {
        if (rawCode.isBlank() || scanInProgress) return
        scanInProgress = true
        binding.inputStockInScan.verifying("入库处理中...")
        renderRecords(viewModel.stockInRecords.value.orEmpty())
        viewModel.stockIn(rawCode)
    }

    private fun renderRecords(records: List<ProductStockInRecordDto>) {
        adapter.submitListWithDiff(records)
        binding.emptyProducts.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        binding.tvProductSummary.text = "本次入库 ${records.size} 条"
        binding.inputStockInScan.setInputEnabled(!scanInProgress)
        if (!scanInProgress) binding.inputStockInScan.setScanOnly(true)
    }

    private fun bindRecord(
        itemBinding: ItemProductStockInBinding,
        item: ProductStockInRecordDto,
        position: Int,
    ) {
        itemBinding.tvProductIndex.text = (position + 1).toString()
        itemBinding.tvProductCode.text = "物料码：${item.materialCode.orDash()}"
        itemBinding.tvProductName.text = "物料名称：${item.partName.orDash()}"
        itemBinding.tvPlanCode.text = "物料 ID：${item.proMatId.orDash()}"
        itemBinding.tvProductSource.visibility = View.GONE
        itemBinding.btnRemoveProduct.visibility = View.GONE
    }

    private fun focusScanner() {
        binding.inputStockInScan.post {
            binding.inputStockInScan.requestInputFocus(showKeyboard = false)
        }
    }

    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"
}
