package com.jj.zds.feature.materialtrace

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.state.JiangErrorMode
import com.jj.zds.R
import com.jj.zds.data.dto.materialtrace.MaterialTraceBindDto
import com.jj.zds.data.dto.materialtrace.MaterialTraceDetailDto
import com.jj.zds.data.dto.materialtrace.MaterialTraceInfoDto
import com.jj.zds.data.dto.materialtrace.MaterialTraceNodeDto
import com.jj.zds.databinding.ActivityMaterialTraceBinding

class MaterialTraceActivity :
    JiangToolbarStateVmActivity<ActivityMaterialTraceBinding, MaterialTraceViewModel>() {

    private var queryInProgress = false

    override fun createViewBinding(inflater: LayoutInflater) = ActivityMaterialTraceBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.inputTraceScan.setScanOnly(true)
        (binding.inputTraceScan.focusedInputView() as? EditText)?.isCursorVisible = true
        binding.inputTraceScan.onTextDebounce(delayMillis = 260L, listener = ::queryTrace)
        binding.inputTraceScan.onInputDone(::queryTrace)
        binding.inputTraceScan.onTextChanged { text ->
            if (text.isBlank() && !queryInProgress) clearResult()
        }
        clearResult()
        focusScanner()
    }

    override fun initPageObserver() {
        viewModel.traceDetail.observe(this) { detail ->
            queryInProgress = false
            binding.inputTraceScan.showSuccess("追溯记录已加载")
            renderTrace(detail)
            focusScanner()
        }
    }

    override fun toolbarTitle(): CharSequence = "物料追溯"

    override fun toolbarSubtitle(): CharSequence = "查看物料履历"

    override fun showBackButton(): Boolean = true

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        queryInProgress = false
        clearResult()
        binding.inputTraceScan.showError(message).selectAll()
        focusScanner()
    }

    private fun queryTrace(rawCode: String) {
        val qrCode = rawCode.trim()
        if (qrCode.isBlank() || queryInProgress) return
        queryInProgress = true
        clearResult(keepInput = true)
        binding.inputTraceScan.verifying("追溯查询中...")
        viewModel.query(qrCode)
    }

    private fun renderTrace(detail: MaterialTraceDetailDto) {
        binding.cardResult.visibility = View.VISIBLE
        binding.emptyTrace.visibility = View.GONE
        renderInfo(detail.info)
        renderBindings(detail.bindings)
        renderNodes(detail.nodes)
    }

    private fun renderInfo(info: MaterialTraceInfoDto) {
        binding.tvTraceCode.text = info.materialCode.orDash()
        binding.tvTraceType.text = when (info.materialType) {
            1 -> "物料"
            2 -> "成品"
            3 -> "物料 / 成品"
            else -> "追溯对象"
        }
        binding.tvTraceStatus.visibility = if (info.materialStatusName.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvTraceStatus.text = info.materialStatusName.orEmpty()
        binding.tvTraceStatus.background = pillBackground(statusColor(info.materialStatusName))
        binding.tvTraceStatus.setTextColor(statusColor(info.materialStatusName))
        val traceName = listOf(info.categoryName, info.productName).filterNotNull().filter { it.isNotBlank() }
            .joinToString(" · ")
        binding.tvTraceName.visibility = if (traceName.isBlank()) View.GONE else View.VISIBLE
        binding.tvTraceName.text = traceName
        binding.layoutTraceFields.removeAllViews()
        addField("规格", info.specification)
        addField("型号", info.seriesName)
        addField("生产单号", info.orderCode)
        addField("批次", info.lot)
        addField("所在位置", info.location)
        addField("线体", info.lineName)
        addField("关键件数量", info.keyMatCount?.toString())
        addField("流转单号", info.circulationCode)
        addLinkedField("关联成品", info.bindProCode, info.bindProQrCode)
    }

    private fun addField(label: String, value: String?) {
        val displayValue = value?.takeIf { it.isNotBlank() } ?: return
        binding.layoutTraceFields.addView(
            TextView(this).apply {
                text = "$label：$displayValue"
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@MaterialTraceActivity, com.jj.ui.R.color.j_color_text_secondary))
                setPadding(dp(10), dp(9), dp(10), dp(9))
                background = roundedBackground(Color.rgb(244, 247, 251), 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = dp(6) }
            },
        )
    }

    private fun addLinkedField(label: String, value: String?, qrCode: String?) {
        val displayValue = value?.takeIf { it.isNotBlank() } ?: return
        val traceCode = qrCode?.takeIf { it.isNotBlank() }
        if (traceCode == null) {
            addField(label, displayValue)
            return
        }
        binding.layoutTraceFields.addView(
            TextView(this).apply {
                text = "$label：$displayValue  ›"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@MaterialTraceActivity, com.jj.ui.R.color.j_color_primary))
                setPadding(dp(10), dp(9), dp(10), dp(9))
                background = roundedBackground(Color.rgb(247, 250, 255), 8, Color.rgb(190, 211, 250))
                setOnClickListener {
                    binding.inputTraceScan.setText(traceCode)
                    queryTrace(traceCode)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = dp(6) }
            },
        )
    }

    private fun renderBindings(bindings: List<MaterialTraceBindDto>) {
        binding.layoutTraceBindings.removeAllViews()
        binding.cardTraceBindings.visibility = if (bindings.isEmpty()) View.GONE else View.VISIBLE
        bindings.forEachIndexed { index, item ->
            binding.layoutTraceBindings.addView(bindingRow(index, item))
        }
    }

    private fun bindingRow(index: Int, item: MaterialTraceBindDto): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = roundedBackground(Color.rgb(247, 250, 255), 8, Color.rgb(216, 228, 251))
            addView(TextView(context).apply {
                text = (index + 1).toString()
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_primary))
                background = pillBackground(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_primary))
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            })
            addView(TextView(context).apply {
                text = "${item.productName.orDash()}\n${item.categoryName.orDash()} · ${item.materialCode.orDash()}"
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                setLineSpacing(dp(3).toFloat(), 1f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(9) }
            })
            addView(TextView(context).apply {
                text = if (item.isBind) "已绑定" else "历史绑定"
                textSize = 11f
                setTextColor(statusColor(if (item.isBind) "已绑定" else "历史"))
                background = pillBackground(statusColor(if (item.isBind) "已绑定" else "历史"))
            })
            item.materialQrCode?.takeIf { it.isNotBlank() }?.let { code ->
                setOnClickListener { binding.inputTraceScan.setText(code); queryTrace(code) }
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(7) }
        }
    }

    private fun renderNodes(nodes: List<MaterialTraceNodeDto>) {
        binding.layoutTraceNodes.removeAllViews()
        binding.tvTraceNodeCount.text = "共 ${nodes.size} 条 · 最新优先"
        if (nodes.isEmpty()) {
            binding.layoutTraceNodes.addView(TextView(this).apply {
                text = "暂无关键节点履历"
                gravity = Gravity.CENTER
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@MaterialTraceActivity, com.jj.ui.R.color.j_color_text_secondary))
                setPadding(0, dp(20), 0, dp(20))
            })
            return
        }
        nodes.forEach { node -> binding.layoutTraceNodes.addView(nodeCard(node)) }
    }

    private fun nodeCard(node: MaterialTraceNodeDto): View {
        val color = statusColor(node.status)
        val detail = TextView(this).apply {
            text = buildList {
                listOf(node.account, node.operator)
                    .filterNotNull()
                    .filter { it.isNotBlank() }
                    .joinToString(" / ")
                    .takeIf { it.isNotBlank() }
                    ?.let { add("操作人：$it") }
                node.orderCode?.takeIf { it.isNotBlank() }?.let { add("关联单号：$it") }
                node.remark?.takeIf { it.isNotBlank() }?.let { add("记录说明：$it") }
            }.joinToString("\n")
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MaterialTraceActivity, com.jj.ui.R.color.j_color_text_secondary))
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(dp(12), dp(9), dp(12), dp(11))
            visibility = View.GONE
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(Color.WHITE, 9, Color.rgb(225, 231, 239))
            val summary = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(11), dp(12), dp(11))
                addView(View(context).apply {
                    background = pillBackground(color)
                    layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(9) }
                })
                addView(TextView(context).apply {
                    text = node.time.orEmpty()
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
                    layoutParams = LinearLayout.LayoutParams(dp(82), LinearLayout.LayoutParams.WRAP_CONTENT)
                })
                addView(TextView(context).apply {
                    text = node.title.orEmpty()
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                node.status?.takeIf { it.isNotBlank() }?.let { status ->
                    addView(TextView(context).apply {
                        text = "状态：$status"
                        textSize = 11f
                        setTextColor(color)
                        setPadding(dp(7), dp(3), dp(7), dp(3))
                        background = pillBackground(color)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply { marginStart = dp(6) }
                    })
                }
                addView(TextView(context).apply { text = "⌄"; textSize = 18f; setTextColor(color) })
                setOnClickListener { detail.visibility = if (detail.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
            }
            addView(summary)
            addView(detail)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
        }
    }

    private fun clearResult(keepInput: Boolean = false) {
        binding.cardResult.visibility = View.GONE
        binding.emptyTrace.visibility = View.VISIBLE
        if (!keepInput) binding.inputTraceScan.clearState()
    }

    private fun focusScanner() = binding.inputTraceScan.post { binding.inputTraceScan.requestInputFocus(showKeyboard = false) }

    private fun statusColor(status: String?): Int = when {
        status.orEmpty().contains("异常") || status.orEmpty().contains("NG", true) || status.orEmpty().contains("失败") -> Color.rgb(220, 38, 38)
        status.orEmpty().contains("待") || status.orEmpty().contains("换") || status.orEmpty().contains("历史") -> Color.rgb(217, 119, 6)
        else -> Color.rgb(22, 163, 74)
    }

    private fun pillBackground(color: Int) = roundedBackground(Color.argb(28, Color.red(color), Color.green(color), Color.blue(color)), 999)

    private fun roundedBackground(fill: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radius).toFloat()
        setColor(fill)
        stroke?.let { setStroke(dp(1), it) }
    }

    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
