package com.jj.zds.feature.aging

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.dialog.JSingleChoiceDialog
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.data.dto.aging.AgingEndDetailDto
import com.jj.zds.data.dto.aging.AgingProductDto
import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import com.jj.zds.databinding.ActivityAgingBinding
import com.jj.zds.databinding.ItemAgingProductBinding

class AgingActivity : JiangToolbarStateVmActivity<ActivityAgingBinding, AgingViewModel>() {

    private var scanInProgress = false
    private var submitInProgress = false
    private var currentMode = AgingMode.START
    private var activeOperation = AgingOperation.NONE
    private var waitingNgPhenomena = false

    private val adapter = JBindingAdapter(
        inflate = ItemAgingProductBinding::inflate,
        onBind = ::bindProduct,
    )

    override fun createViewBinding(inflater: LayoutInflater): ActivityAgingBinding {
        return ActivityAgingBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerProducts.setupLinear(adapter)
        binding.recyclerProducts.addDefaultDivider()
        adapter.setOnChildClickListener(R.id.btnRemoveProduct) { item, _ ->
            viewModel.remove(item)
            focusScanner()
        }

        binding.inputAgingScan.setScanOnly(true)
        binding.inputAgingScan.onTextDebounce(delayMillis = 260L, listener = ::submitScan)
        binding.inputAgingScan.onInputDone(::submitScan)
        binding.inputEndAgingScan.setScanOnly(true)
        binding.inputEndAgingScan.onTextDebounce(delayMillis = 260L, listener = ::submitEndScan)
        binding.inputEndAgingScan.onInputDone(::submitEndScan)
        binding.btnModeStart.setOnClickListener { switchMode(AgingMode.START) }
        binding.btnModeEnd.setOnClickListener { switchMode(AgingMode.END) }
        binding.btnStartAging.setOnClickListener {
            if (submitInProgress) return@setOnClickListener
            submitInProgress = true
            activeOperation = AgingOperation.START_SUBMIT
            renderProducts(viewModel.pendingProducts.value.orEmpty())
            viewModel.submit()
        }
        binding.btnEndOk.setOnClickListener {
            submitEnd(AgingViewModel.RESULT_OK, "")
        }
        binding.btnEndNg.setOnClickListener {
            if (viewModel.endDetail.value == null || submitInProgress) return@setOnClickListener
            waitingNgPhenomena = true
            activeOperation = AgingOperation.LOAD_NG_PHENOMENA
            viewModel.loadNgPhenomena()
        }

        renderProducts(viewModel.pendingProducts.value.orEmpty())
        renderEndDetail(viewModel.endDetail.value)
        switchMode(AgingMode.START, force = true)
    }

    override fun initPageObserver() {
        viewModel.pendingProducts.observe(this, ::renderProducts)
        viewModel.scanMergeResult.observe(this) { result ->
            scanInProgress = false
            activeOperation = AgingOperation.NONE
            renderProducts(viewModel.pendingProducts.value.orEmpty())
            binding.inputAgingScan.clearText()
            val message = when {
                result.addedCount > 0 && result.duplicateCount > 0 ->
                    "已加入${result.addedCount}台，过滤${result.duplicateCount}台重复PRO"
                result.addedCount > 0 -> "校验通过，已加入${result.addedCount}台产品"
                result.duplicateCount > 0 -> "扫描结果中的PRO已在待开始列表中"
                else -> "扫描结果中没有有效PRO码"
            }
            if (result.addedCount > 0) {
                binding.inputAgingScan.showSuccess(message)
            } else {
                binding.inputAgingScan.showError(message)
            }
            focusScanner()
        }
        viewModel.startDone.observe(this) { count ->
            submitInProgress = false
            activeOperation = AgingOperation.NONE
            binding.inputAgingScan.clearText().showSuccess("$count 台产品已开始老化")
            JToast.show("$count 台产品已开始老化")
            focusScanner()
        }
        viewModel.endDetail.observe(this) { detail ->
            if (activeOperation == AgingOperation.END_SCAN) {
                scanInProgress = false
                activeOperation = AgingOperation.NONE
                binding.inputEndAgingScan.clearText().showSuccess("校验通过，可结束老化")
            }
            renderEndDetail(detail)
            if (currentMode == AgingMode.END) focusEndScanner()
        }
        viewModel.ngPhenomenaItems.observe(this) { items ->
            if (!waitingNgPhenomena) return@observe
            waitingNgPhenomena = false
            activeOperation = AgingOperation.NONE
            showNgPhenomenaDialog(items)
        }
        viewModel.endDone.observe(this) {
            submitInProgress = false
            activeOperation = AgingOperation.NONE
            clearEndLayout()
            JToast.show("老化结束")
            focusEndScanner()
        }
    }

    override fun toolbarTitle(): CharSequence = "老化作业"

    override fun toolbarSubtitle(): CharSequence = "产品老化开始与结束"

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        super.onUiError(code, message, throwable)
        scanInProgress = false
        submitInProgress = false
        renderProducts(viewModel.pendingProducts.value.orEmpty())
        renderEndDetail(viewModel.endDetail.value)
        when (activeOperation) {
            AgingOperation.START_SCAN -> {
                binding.inputAgingScan.showError(message)
                focusScanner()
            }
            AgingOperation.END_SCAN -> {
                binding.inputEndAgingScan.showError(message)
                focusEndScanner()
            }
            AgingOperation.LOAD_NG_PHENOMENA -> {
                waitingNgPhenomena = false
                showErrorDialog("获取不良现象失败", message)
            }
            AgingOperation.START_SUBMIT -> showErrorDialog("开始老化失败", message)
            AgingOperation.END_SUBMIT -> showErrorDialog("结束老化失败", message)
            AgingOperation.NONE -> showErrorDialog("操作失败", message)
        }
        activeOperation = AgingOperation.NONE
    }

    private fun showErrorDialog(title: String, message: String) {
            JConfirmDialog(this, this)
                .setTitle(title)
                .setMessage(message)
                .setConfirmText("知道了")
                .setShowCancelButton(false)
                .setOnDismiss(::focusActiveScanner)
                .show()
    }

    private fun submitScan(rawCode: String) {
        val code = rawCode.trim()
        if (code.isBlank() || scanInProgress || submitInProgress) return
        scanInProgress = true
        activeOperation = AgingOperation.START_SCAN
        binding.inputAgingScan.verifying("扫码校验中...")
        renderProducts(viewModel.pendingProducts.value.orEmpty())
        viewModel.scan(code)
    }

    private fun submitEndScan(rawCode: String) {
        val code = rawCode.trim()
        if (code.isBlank() || scanInProgress || submitInProgress) return
        scanInProgress = true
        activeOperation = AgingOperation.END_SCAN
        binding.inputEndAgingScan.verifying("扫码校验中...")
        renderEndDetail(viewModel.endDetail.value)
        viewModel.scanEnd(code)
    }

    private fun renderProducts(products: List<AgingProductDto>) {
        adapter.submitListWithDiff(products)
        binding.emptyProducts.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
        binding.btnStartAging.isEnabled = products.isNotEmpty() && !scanInProgress && !submitInProgress
        binding.btnStartAging.alpha = if (binding.btnStartAging.isEnabled) 1f else 0.6f
        binding.btnStartAging.text = buildButtonText(products)
        binding.inputAgingScan.setInputEnabled(!scanInProgress && !submitInProgress)
        if (!scanInProgress && !submitInProgress) binding.inputAgingScan.setScanOnly(true)
    }

    private fun renderEndDetail(detail: AgingEndDetailDto?) {
        val hasDetail = detail != null
        binding.emptyEndDetail.visibility = if (hasDetail) View.GONE else View.VISIBLE
        binding.cardEndDetail.visibility = if (hasDetail) View.VISIBLE else View.GONE
        binding.btnEndOk.isEnabled = hasDetail && !scanInProgress && !submitInProgress
        binding.btnEndNg.isEnabled = hasDetail && !scanInProgress && !submitInProgress
        binding.btnEndOk.alpha = if (binding.btnEndOk.isEnabled) 1f else 0.6f
        binding.btnEndNg.alpha = if (binding.btnEndNg.isEnabled) 1f else 0.6f
        binding.inputEndAgingScan.setInputEnabled(!scanInProgress && !submitInProgress)
        if (!scanInProgress && !submitInProgress) binding.inputEndAgingScan.setScanOnly(true)
        if (detail == null) return
        binding.tvEndProductCode.text = detail.code.orDash()
        binding.tvEndProductName.text = listOfNotNull(
            detail.partName?.takeIf(String::isNotBlank),
            detail.partCode?.takeIf(String::isNotBlank),
        ).joinToString(" / ").ifBlank { "产品信息未知" }
        binding.tvEndPlan.text = "生产计划：${detail.planOrderNo.orDash()}"
        binding.tvEndStartTime.text = "开始时间：${detail.agingStartTime.orDash()}"
        binding.tvEndProgress.text = "当前计划老化 OK：${detail.agingOkQty} / ${detail.agingQty} 台"
    }

    private fun submitEnd(result: String, ngPhenomena: String) {
        if (submitInProgress || viewModel.endDetail.value == null) return
        submitInProgress = true
        activeOperation = AgingOperation.END_SUBMIT
        renderEndDetail(viewModel.endDetail.value)
        viewModel.endAging(result, ngPhenomena)
    }

    private fun showNgPhenomenaDialog(items: List<DebugDictItemDto>) {
        if (items.isEmpty()) {
            JToast.show("不良现象列表为空")
            focusEndScanner()
            return
        }
        JSingleChoiceDialog<DebugDictItemDto>(this, this)
            .setTitle("选择不良现象")
            .setItems(items) { it.label ?: it.value ?: it.id.orDash() }
            .setOnConfirm { selected ->
                val value = selected?.value ?: selected?.label
                if (value.isNullOrBlank()) {
                    JToast.show("请选择不良现象")
                    focusEndScanner()
                } else {
                    submitEnd(AgingViewModel.RESULT_NG, value)
                }
            }
            .show()
            .setOnDismiss(::focusEndScanner)
    }

    private fun switchMode(mode: AgingMode, force: Boolean = false) {
        if (!force && currentMode == mode) return
        currentMode = mode
        scanInProgress = false
        submitInProgress = false
        activeOperation = AgingOperation.NONE
        waitingNgPhenomena = false
        viewModel.clearAll()
        binding.inputAgingScan.clearText().clearState()
        binding.inputEndAgingScan.clearText().clearState()
        renderProducts(emptyList())
        renderEndDetail(null)
        binding.layoutStartMode.visibility = if (mode == AgingMode.START) View.VISIBLE else View.GONE
        binding.layoutEndMode.visibility = if (mode == AgingMode.END) View.VISIBLE else View.GONE
        updateModeButtons()
        if (mode == AgingMode.START) focusScanner() else focusEndScanner()
    }

    private fun updateModeButtons() {
        val startSelected = currentMode == AgingMode.START
        binding.btnModeStart.setBackgroundResource(
            if (startSelected) R.drawable.aging_bg_mode_selected else R.drawable.pda_bg_filter_unselected,
        )
        binding.btnModeEnd.setBackgroundResource(
            if (startSelected) R.drawable.pda_bg_filter_unselected else R.drawable.aging_bg_mode_selected,
        )
        binding.btnModeStart.setTextColor(
            ContextCompat.getColor(
                this,
                if (startSelected) com.jj.ui.R.color.j_color_text_white else com.jj.ui.R.color.j_color_text_secondary,
            ),
        )
        binding.btnModeEnd.setTextColor(
            ContextCompat.getColor(
                this,
                if (startSelected) com.jj.ui.R.color.j_color_text_secondary else com.jj.ui.R.color.j_color_text_white,
            ),
        )
    }

    private fun clearEndLayout() {
        binding.inputEndAgingScan.clearText().clearState()
        renderEndDetail(null)
    }

    private fun buildButtonText(products: List<AgingProductDto>): CharSequence {
        val title = if (submitInProgress) "开始老化中..." else "开始老化"
        val summary = AgingPendingListRules.summary(products)
        return SpannableString("$title\n$summary").apply {
            setSpan(
                RelativeSizeSpan(0.68f),
                title.length + 1,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    private fun bindProduct(
        itemBinding: ItemAgingProductBinding,
        item: AgingProductDto,
        position: Int,
    ) {
        itemBinding.tvProductIndex.text = (position + 1).toString()
        itemBinding.tvProductCode.text = item.materialCode.orDash()
        itemBinding.tvProductName.text = listOfNotNull(
            item.partName?.takeIf(String::isNotBlank),
            item.partCode?.takeIf(String::isNotBlank),
        ).joinToString(" / ").ifBlank { "产品信息未知" }
        itemBinding.tvPlanCode.text = "生产计划：${item.planOrderCode.orDash()}"
    }

    private fun focusScanner() {
        binding.inputAgingScan.post {
            binding.inputAgingScan.requestInputFocus(showKeyboard = false)
        }
    }

    private fun focusEndScanner() {
        binding.inputEndAgingScan.post {
            binding.inputEndAgingScan.requestInputFocus(showKeyboard = false)
        }
    }

    private fun focusActiveScanner() {
        if (currentMode == AgingMode.START) focusScanner() else focusEndScanner()
    }

    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"

    private enum class AgingMode {
        START,
        END,
    }

    private enum class AgingOperation {
        NONE,
        START_SCAN,
        START_SUBMIT,
        END_SCAN,
        END_SUBMIT,
        LOAD_NG_PHENOMENA,
    }
}
