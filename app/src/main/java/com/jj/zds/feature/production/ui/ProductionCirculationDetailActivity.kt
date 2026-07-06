package com.jj.zds.feature.production.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.data.dto.productionplan.ProductionCirculationMaterialDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationPrintLogDto
import com.jj.zds.databinding.ActivityProductionCirculationDetailBinding
import com.jj.zds.viewmodel.ZdsViewModel

class ProductionCirculationDetailActivity :
    JiangToolbarStateVmActivity<ActivityProductionCirculationDetailBinding, ZdsViewModel>() {

    private val circulationId by lazy { intent.getStringExtra(EXTRA_ID).orEmpty() }

    override fun createViewBinding(inflater: LayoutInflater) =
        ActivityProductionCirculationDetailBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        renderProducts(emptyList())
        renderPrintLogs(emptyList())
    }

    override fun initPageObserver() {
        viewModel.productionCirculationInfo.observe(this) { info ->
            binding.tvCode.text = info.circulationCode.orDash()
            binding.tvBaseInfo.text = buildString {
                appendLine("数量：${info.num.coerceAtLeast(0)}")
                appendLine("生产计划号：${info.orderCode.orDash()}")
                appendLine("产品编码：${info.partCode.orDash()}")
                appendLine("产品名称：${info.partName.orDash()}")
                appendLine("产品型号：${info.specification.orDash()}")
                appendLine("打印次数：${info.printCount.coerceAtLeast(0)}")
                appendLine("首次打印时间：${info.generateTime.orDash()}")
                append("最近打印时间：${info.printTime.orDash()}")
            }
        }
        viewModel.productionCirculationMaterials.observe(this, ::renderProducts)
        viewModel.productionCirculationPrintLogs.observe(this, ::renderPrintLogs)
    }

    override fun initData() {
        if (circulationId.isBlank()) {
            JToast.show("流转单ID为空")
            finish()
            return
        }
        viewModel.loadProductionCirculationInfo(circulationId)
        viewModel.loadProductionCirculationMaterials(circulationId)
        viewModel.loadProductionCirculationPrintLogs(circulationId)
    }

    override fun toolbarTitle(): CharSequence = "流转单详情"
    override fun showBackButton(): Boolean = true
    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"

    private fun renderProducts(items: List<ProductionCirculationMaterialDto>) {
        binding.layoutProducts.removeAllViews()
        if (items.isEmpty()) {
            binding.layoutProducts.addView(createEmptyView("暂无产品明细"))
            return
        }
        items.forEachIndexed { index, item ->
            binding.layoutProducts.addView(
                createInfoCard(
                    "PRO：${item.materialCode.orDash()}\n" +
                        "测试状态：${item.testStatus()}\n测试完成时间：${item.createTime.orDash()}",
                ),
            )
        }
    }

    private fun renderPrintLogs(items: List<ProductionCirculationPrintLogDto>) {
        binding.layoutRecords.removeAllViews()
        if (items.isEmpty()) {
            binding.layoutRecords.addView(createEmptyView("暂无打印记录"))
            return
        }
        items.sortedBy { it.printTime.orEmpty() }.forEachIndexed { index, item ->
            val action = if (index == 0) "首次打印" else "重新打印"
            binding.layoutRecords.addView(
                createInfoCard(
                    "$action\n操作人：${item.createByName.orDash()}\n" +
                        "时间：${item.printTime.orDash()}\n备注：-",
                ),
            )
        }
    }

    private fun createInfoCard(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
            setBackgroundResource(R.drawable.pda_bg_card)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(8) }
        }
    }

    private fun createEmptyView(text: String): TextView {
        return createInfoCard(text).apply {
            gravity = android.view.Gravity.CENTER
            minHeight = dp(88)
            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
        }
    }

    private fun ProductionCirculationMaterialDto.testStatus(): String {
        return when (okType) {
            1 -> "通过"
            0 -> "未通过"
            else -> okType?.toString() ?: "-"
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        private const val EXTRA_ID = "extra_circulation_id"

        fun start(context: Context, id: String) {
            context.startActivity(
                Intent(context, ProductionCirculationDetailActivity::class.java)
                    .putExtra(EXTRA_ID, id),
            )
        }
    }
}
