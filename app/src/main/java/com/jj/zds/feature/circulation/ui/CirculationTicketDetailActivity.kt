package com.jj.zds.feature.circulation.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.data.dto.productionplan.ProductionCirculationInfoDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationMaterialDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationPrintLogDto
import com.jj.zds.databinding.ActivityCirculationTicketDetailBinding
import com.jj.zds.viewmodel.ZdsViewModel

class CirculationTicketDetailActivity :
    JiangToolbarStateVmActivity<ActivityCirculationTicketDetailBinding, ZdsViewModel>() {

    private val circulationId by lazy { intent.getStringExtra(EXTRA_ID).orEmpty() }
    private var titleCode = ""

    override fun createViewBinding(inflater: LayoutInflater): ActivityCirculationTicketDetailBinding {
        return ActivityCirculationTicketDetailBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        renderProducts(emptyList())
        renderLogs(emptyList())
    }

    override fun initPageObserver() {
        viewModel.productionCirculationInfo.observe(this, ::renderInfo)
        viewModel.productionCirculationMaterials.observe(this, ::renderProducts)
        viewModel.productionCirculationPrintLogs.observe(this, ::renderLogs)
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

    override fun toolbarTitle(): CharSequence = "票面详情"

    override fun toolbarSubtitle(): CharSequence = titleCode

    override fun showBackButton(): Boolean = true

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    private fun renderInfo(info: ProductionCirculationInfoDto) {
        titleCode = info.circulationCode.orDash()
        toolbar.setSubtitleText(titleCode)
        binding.tvBaseInfo.text = buildString {
            appendLine("流转单号：${info.circulationCode.orDash()}")
            appendLine("状态：${if (info.valid == true) "有效" else "失效"}")
            appendLine("产品名称：${info.partName.orDash()}")
            appendLine("产品号：${info.partCode.orDash()}")
            appendLine("产品型号：${info.specification.orDash()}")
            appendLine("当前数量：${info.num} 台")
            append("备注：${info.remark.orDash()}")
        }
        binding.tvProductionInfo.text = buildString {
            appendLine("生产计划号：${info.orderCode.orDash()}")
            appendLine("工艺版本：${info.tecVer.orDash()}")
            appendLine("生产时间：${info.generateTime.orDash()}")
            appendLine("作业者：${info.createByName.orDash()}")
            append("生产线体：${info.lineName.orDash()}")
        }
    }

    private fun renderProducts(items: List<ProductionCirculationMaterialDto>) {
        binding.tvSnCount.text = "${items.size} 台"
        binding.layoutSnList.removeAllViews()
        binding.tvSnEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        items.forEachIndexed { index, item ->
            binding.layoutSnList.addView(
                createRow("${index + 1}", item.materialCode.orDash(), item.okType.statusText()),
            )
        }
    }

    private fun renderLogs(items: List<ProductionCirculationPrintLogDto>) {
        binding.layoutPrintLogs.removeAllViews()
        binding.tvLogEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        items.sortedByDescending { it.printTime.orEmpty() }.forEachIndexed { index, item ->
            binding.layoutPrintLogs.addView(
                createLog(
                    if (index == items.lastIndex) "首次打印" else "重新打印",
                    "${item.createByName.orDash()} · ${item.printTime.orDash()}",
                ),
            )
        }
    }

    private fun createRow(index: String, code: String, status: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(8), dp(4), dp(8))
            addView(textView(index, secondary = true), LinearLayout.LayoutParams(dp(38), -2))
            addView(textView(code), LinearLayout.LayoutParams(0, -2, 1f))
            addView(textView(status, secondary = true))
        }
    }

    private fun createLog(title: String, subtitle: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundResource(R.drawable.pda_bg_card)
            addView(textView(title))
            addView(textView(subtitle, secondary = true))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) }
        }
    }

    private fun textView(value: String, secondary: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = if (secondary) 12f else 13f
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (secondary) com.jj.ui.R.color.j_color_text_secondary
                    else com.jj.ui.R.color.j_color_text_primary,
                ),
            )
        }
    }

    private fun Int?.statusText(): String = when (this) {
        1 -> "测试OK"
        2 -> "复检OK"
        else -> "归属有效"
    }

    private fun String?.orDash(): String = this?.takeIf(String::isNotBlank) ?: "-"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        private const val EXTRA_ID = "extra_circulation_id"

        fun start(context: Context, id: String) {
            context.startActivity(
                Intent(context, CirculationTicketDetailActivity::class.java).putExtra(EXTRA_ID, id),
            )
        }
    }
}
