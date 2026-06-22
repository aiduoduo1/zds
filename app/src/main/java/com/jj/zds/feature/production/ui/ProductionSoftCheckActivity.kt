package com.jj.zds.feature.production.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.data.dto.productionplan.ProductionSwitchSoftDto
import com.jj.zds.data.dto.productionplan.isSoftCheckComplete
import com.jj.zds.databinding.ActivityProductionSoftCheckBinding
import com.jj.zds.viewmodel.ZdsViewModel

class ProductionSoftCheckActivity :
    JiangToolbarStateVmActivity<ActivityProductionSoftCheckBinding, ZdsViewModel>() {

    private val partCode: String by lazy {
        intent.getStringExtra(EXTRA_PART_CODE).orEmpty()
    }

    private val softItems: List<ProductionSwitchSoftDto> by lazy {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        (intent.getSerializableExtra(EXTRA_SOFT_LIST) as? ArrayList<ProductionSwitchSoftDto>).orEmpty()
    }

    override fun createViewBinding(inflater: LayoutInflater): ActivityProductionSoftCheckBinding {
        return ActivityProductionSoftCheckBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        renderSoftItems()
        binding.btnSubmit.setOnClickListener {
            viewModel.confirmProductionSoftCheck(resolveLineSwiId())
        }
    }

    override fun initPageObserver() {
        viewModel.productionCheckDone.observe(this) {
            JToast.show("软件版本核对提交成功")
            binding.btnSubmit.visibility = View.GONE
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun toolbarTitle(): CharSequence = partCode.ifBlank { "产品号" }

    override fun toolbarSubtitle(): CharSequence = "软件版本核对"

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    private fun renderSoftItems() {
        binding.layoutSoftItems.removeAllViews()
        binding.tvEmpty.visibility = if (softItems.isEmpty()) View.VISIBLE else View.GONE
        binding.btnSubmit.visibility = if (softItems.isSoftCheckComplete()) View.GONE else View.VISIBLE
        softItems.forEachIndexed { index, item ->
            binding.layoutSoftItems.addView(createSoftView(index + 1, item))
        }
    }

    private fun resolveLineSwiId(): String {
        return softItems.firstOrNull { !it.lineSwiId.isNullOrBlank() }?.lineSwiId.orEmpty()
    }

    private fun createSoftView(index: Int, item: ProductionSwitchSoftDto): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundResource(R.drawable.pda_bg_card)
            elevation = dp(3).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(10)
            }

            addView(
                TextView(context).apply {
                    text = "$index. ${item.softGrpLabel.orDash(item.softGrp, item.softCode)}"
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                },
            )
            addView(createInfoLine("版本类别：${item.softGrpLabel.orDash(item.softGrp)}"))
            addView(createInfoLine("版本代码：${item.softCode.orDash()}"))
            addView(createStatusLine(if (item.status == STATUS_DONE) "已核对" else "待核对"))
        }
    }

    private fun createInfoLine(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(6)
            }
        }
    }

    private fun createStatusLine(value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(10), 0)
            setBackgroundResource(R.drawable.pda_bg_quantity_focus)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(38),
            ).apply {
                topMargin = dp(8)
            }

            addView(
                TextView(context).apply {
                    text = "状态"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                },
            )
            addView(
                TextView(context).apply {
                    text = value
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_primary))
                },
            )
        }
    }

    private fun String?.orDash(vararg fallback: String?): String {
        return sequenceOf(this, *fallback).firstOrNull { !it.isNullOrBlank() } ?: "-"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    companion object {
        private const val EXTRA_PART_CODE = "extra_part_code"
        private const val EXTRA_SOFT_LIST = "extra_soft_list"
        private const val STATUS_DONE = 1

        fun start(
            context: Context,
            partCode: String,
            softList: List<ProductionSwitchSoftDto>,
        ) {
            context.startActivity(
                Intent(context, ProductionSoftCheckActivity::class.java)
                    .putExtra(EXTRA_PART_CODE, partCode)
                    .putExtra(EXTRA_SOFT_LIST, ArrayList(softList)),
            )
        }
    }
}