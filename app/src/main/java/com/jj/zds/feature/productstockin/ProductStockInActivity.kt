package com.jj.zds.feature.productstockin

import android.os.Bundle
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
import com.jj.zds.databinding.ActivityProductStockInBinding
import com.jj.zds.databinding.ItemProductStockInBinding

class ProductStockInActivity :
    JiangToolbarStateVmActivity<ActivityProductStockInBinding, ProductStockInViewModel>() {

    private var scanInProgress = false
    private var submitInProgress = false

    private val adapter = JBindingAdapter(
        inflate = ItemProductStockInBinding::inflate,
        onBind = ::bindProduct,
    )

    override fun createViewBinding(inflater: LayoutInflater): ActivityProductStockInBinding {
        return ActivityProductStockInBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerProducts.setupLinear(adapter)
        binding.recyclerProducts.addDefaultDivider()
        adapter.setOnChildClickListener(R.id.btnRemoveProduct) { item, _ ->
            viewModel.remove(item)
            focusScanner()
        }
        binding.inputStockInScan.setScanOnly(true)
        binding.inputStockInScan.onTextDebounce(delayMillis = 260L, listener = ::submitScan)
        binding.inputStockInScan.onInputDone(::submitScan)
        binding.btnClear.setOnClickListener {
            viewModel.clear()
            binding.inputStockInScan.clearText().clearState()
            focusScanner()
        }
        binding.btnConfirmStockIn.setOnClickListener {
            if (submitInProgress) return@setOnClickListener
            submitInProgress = true
            renderProducts(viewModel.pendingProducts.value.orEmpty())
            viewModel.confirm()
        }
        renderProducts(viewModel.pendingProducts.value.orEmpty())
        focusScanner()
    }

    override fun initPageObserver() {
        viewModel.pendingProducts.observe(this, ::renderProducts)
        viewModel.scanFeedback.observe(this) { feedback ->
            scanInProgress = false
            binding.inputStockInScan.clearText()
            if (feedback.isError) {
                binding.inputStockInScan.showError(feedback.message)
            } else {
                binding.inputStockInScan.showSuccess(feedback.message)
            }
            renderProducts(viewModel.pendingProducts.value.orEmpty())
            focusScanner()
        }
        viewModel.confirmResult.observe(this, ::handleConfirmResult)
    }

    override fun toolbarTitle(): CharSequence = "成品入库"

    override fun toolbarSubtitle(): CharSequence = "扫描待入库产品并统一确认"

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    private fun submitScan(rawCode: String) {
        if (rawCode.isBlank() || scanInProgress || submitInProgress) return
        scanInProgress = true
        binding.inputStockInScan.verifying("扫码校验中...")
        renderProducts(viewModel.pendingProducts.value.orEmpty())
        viewModel.scan(rawCode)
    }

    private fun handleConfirmResult(result: ProductStockInConfirmResult) {
        submitInProgress = false
        binding.inputStockInScan.clearText().clearState()
        renderProducts(result.blocked)
        if (result.blockedPlans.isEmpty()) {
            JToast.show("${result.accepted.size} 台成品已确认入库，Mock 入库记录已生成")
            focusScanner()
            return
        }
        val blockedMessage = buildString {
            if (result.accepted.isNotEmpty()) {
                append("已成功入库 ${result.accepted.size} 台；")
            }
            append("${result.blocked.size} 台暂未入库。\n\n")
            result.blockedPlans.forEach { gate ->
                append(gate.planCode)
                append("：老化 OK ${gate.agingOkQty}/${gate.targetQty} 台，还差 ")
                append((gate.targetQty - gate.agingOkQty).coerceAtLeast(0))
                append(" 台\n")
            }
        }.trim()
        JConfirmDialog(this, this)
            .setTitle("部分生产计划未达到入库条件")
            .setMessage(blockedMessage)
            .setConfirmText("知道了")
            .setShowCancelButton(false)
            .setOnDismiss(::focusScanner)
            .show()
    }

    private fun renderProducts(products: List<ProductStockInProduct>) {
        adapter.submitListWithDiff(products)
        binding.emptyProducts.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
        binding.tvProductSummary.text = "待入库 ${products.size} 台 · ${products.map { it.planCode }.distinct().size} 个生产计划"
        binding.btnConfirmStockIn.text = if (submitInProgress) "入库处理中..." else "确认入库（${products.size}台）"
        binding.btnConfirmStockIn.isEnabled = products.isNotEmpty() && !scanInProgress && !submitInProgress
        binding.btnConfirmStockIn.alpha = if (binding.btnConfirmStockIn.isEnabled) 1f else 0.6f
        binding.btnClear.isEnabled = products.isNotEmpty() && !submitInProgress
        binding.inputStockInScan.setInputEnabled(!scanInProgress && !submitInProgress)
        if (!scanInProgress && !submitInProgress) binding.inputStockInScan.setScanOnly(true)
    }

    private fun bindProduct(
        itemBinding: ItemProductStockInBinding,
        item: ProductStockInProduct,
        position: Int,
    ) {
        itemBinding.tvProductIndex.text = (position + 1).toString()
        itemBinding.tvProductCode.text = item.proCode
        itemBinding.tvProductName.text = "${item.productName} / ${item.partCode}"
        itemBinding.tvPlanCode.text = "生产计划：${item.planCode}"
        itemBinding.tvProductSource.text = "${item.source} · ${item.gateLabel}"
    }

    private fun focusScanner() {
        binding.inputStockInScan.post {
            binding.inputStockInScan.requestInputFocus(showKeyboard = false)
        }
    }
}
