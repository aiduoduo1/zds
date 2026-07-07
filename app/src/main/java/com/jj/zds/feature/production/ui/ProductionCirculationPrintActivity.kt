package com.jj.zds.feature.production.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.data.dto.productionplan.ProductionCirculationItemDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationPrintRequest
import com.jj.zds.data.dto.productionplan.ProductionCirculationProgressDto
import com.jj.zds.databinding.ActivityProductionCirculationPrintBinding
import com.jj.zds.databinding.ItemProductionCirculationBinding
import com.jj.zds.viewmodel.ZdsViewModel

class ProductionCirculationPrintActivity :
    JiangToolbarStateVmActivity<ActivityProductionCirculationPrintBinding, ZdsViewModel>() {

    private val planId by lazy { intent.getStringExtra(EXTRA_PLAN_ID).orEmpty() }
    private val planNo by lazy { intent.getStringExtra(EXTRA_PLAN_NO).orEmpty() }
    private var progress: ProductionCirculationProgressDto? = null
    private var printAction = PrintAction.NONE

    private val adapter = JBindingAdapter(
        inflate = ItemProductionCirculationBinding::inflate,
        onBind = ::bindCirculation,
    )

    override fun createViewBinding(inflater: LayoutInflater) =
        ActivityProductionCirculationPrintBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerCirculation.setupLinear(adapter)
        binding.inputPrintQuantity.doAfterTextChanged { renderStatistics() }
        binding.btnPrint.setOnClickListener { printStandard() }
        binding.btnTailPrint.setOnClickListener { confirmTailPrint() }
    }

    override fun initPageObserver() {
        viewModel.productionCirculationProgress.observe(this) { result ->
            progress = result
            if (binding.inputPrintQuantity.text.isNullOrBlank()) {
                binding.inputPrintQuantity.setText(result.unitQuantity.coerceAtLeast(0).toString())
            }
            binding.tvProduct.text = "产品名称：${result.partName.orDash()}    产品型号：${result.specification.orDash()}"
            binding.tvDefaultHint.text = "工艺默认数量 ${result.unitQuantity.coerceAtLeast(0)} 台，可现场修改"
            val items = result.circulationList.orEmpty().sortedByDescending { it.sort }
            adapter.setList(items)
            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerCirculation.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            renderStatistics()
            result.orderCode?.takeIf { it.isNotBlank() }?.let { toolbar.setSubtitleText(it) }
        }
        viewModel.productionCirculationPrintDone.observe(this) {
            val message = when (printAction) {
                PrintAction.STANDARD -> "打印成功"
                PrintAction.TAIL -> "尾箱打印成功"
                PrintAction.REPRINT -> "重新打印成功"
                PrintAction.NONE -> "操作成功"
            }
            printAction = PrintAction.NONE
            JToast.show(message)
            viewModel.loadProductionCirculationProgress(planId)
        }
    }

    override fun initData() {
        if (planId.isBlank()) {
            JToast.show("生产计划ID为空")
            finish()
            return
        }
        viewModel.loadProductionCirculationProgress(planId)
    }

    override fun toolbarTitle(): CharSequence = "流转单打印"
    override fun toolbarSubtitle(): CharSequence = planNo
    override fun showBackButton(): Boolean = true
    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        printAction = PrintAction.NONE
        super.onUiError(code, message, throwable)
    }

    private fun renderStatistics() {
        val pending = progress?.toPrintQuantity?.coerceAtLeast(0) ?: 0
        val quantity = binding.inputPrintQuantity.text?.toString()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val printable = if (quantity > 0) pending / quantity else 0
        val tail = if (quantity > 0) pending % quantity else pending
        binding.tvPendingCount.text = "待打印数量\n$pending"
        binding.tvPrintableCount.text = "可打印张数\n$printable"
        binding.tvTailCount.text = "尾箱数量\n$tail"
    }

    private fun bindCirculation(
        itemBinding: ItemProductionCirculationBinding,
        item: ProductionCirculationItemDto,
        position: Int,
    ) {
        itemBinding.tvCode.text = item.circulationCode.orDash()
        itemBinding.tvType.text = if (item.type == TYPE_TAIL) "尾箱" else "标准"
        itemBinding.tvInfo.text =
            "打印数量：${item.num.coerceAtLeast(0)}    打印次数：${item.printCount.coerceAtLeast(0)}\n最近打印时间：${item.printTime.orDash()}"
        itemBinding.btnDetail.setOnClickListener {
            val id = item.id.orEmpty()
            if (id.isBlank()) JToast.show("流转单ID为空") else ProductionCirculationDetailActivity.start(this, id)
        }
        itemBinding.btnReprint.visibility = if (item.valid) View.VISIBLE else View.GONE
        itemBinding.btnReprint.setOnClickListener {
            val id = item.id.orEmpty()
            if (id.isBlank()) {
                JToast.show("流转单ID为空")
                return@setOnClickListener
            }
            printAction = PrintAction.REPRINT
            viewModel.printProductionCirculation(
                ProductionCirculationPrintRequest(
                    planId = planId,
                    num = item.num.coerceAtLeast(0).toString(),
                    rePrint = true,
                    id = id,
                    type = item.type.takeIf { it > 0 } ?: TYPE_STANDARD,
                ),
            )
        }
    }

    private fun printStandard() {
        val quantity = binding.inputPrintQuantity.text?.toString()?.toIntOrNull() ?: 0
        if (quantity <= 0) {
            JToast.show("请输入本次打印数量")
            return
        }
        val remaining = progress?.toPrintQuantity?.coerceAtLeast(0) ?: 0
        if (quantity > remaining) {
            JToast.show("本次打印数量不能超过待打印数量")
            return
        }
        printAction = PrintAction.STANDARD
        viewModel.printProductionCirculation(
            ProductionCirculationPrintRequest(
                planId = planId,
                num = quantity.toString(),
                rePrint = false,
                id = "",
                type = TYPE_STANDARD,
            ),
        )
    }

    private fun confirmTailPrint() {
        val remaining = progress?.toPrintQuantity?.coerceAtLeast(0) ?: 0
        if (remaining <= 0) {
            JToast.show("暂无待打印产品")
            return
        }
        showConfirmDialog(
            title = "尾箱打印",
            message = "确认将剩余 $remaining 台产品生成一张尾箱流转单？",
            onConfirm = {
                printAction = PrintAction.TAIL
                viewModel.printProductionCirculation(
                    ProductionCirculationPrintRequest(
                        planId = planId,
                        num = remaining.toString(),
                        rePrint = false,
                        id = "",
                        type = TYPE_TAIL,
                    ),
                )
            },
        )
    }

    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"

    private enum class PrintAction {
        NONE,
        STANDARD,
        TAIL,
        REPRINT,
    }

    companion object {
        private const val EXTRA_PLAN_ID = "extra_plan_id"
        private const val EXTRA_PLAN_NO = "extra_plan_no"
        private const val TYPE_STANDARD = 1
        private const val TYPE_TAIL = 2

        fun start(context: Context, planId: String, planNo: String = "") {
            context.startActivity(
                Intent(context, ProductionCirculationPrintActivity::class.java)
                    .putExtra(EXTRA_PLAN_ID, planId)
                    .putExtra(EXTRA_PLAN_NO, planNo),
            )
        }
    }
}
