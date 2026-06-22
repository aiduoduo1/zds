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
import com.jj.zds.data.dto.productionplan.ProductionSwitchCategoryDto
import com.jj.zds.data.dto.productionplan.firstLineSwiId
import com.jj.zds.data.dto.productionplan.isMaterialCheckComplete
import com.jj.zds.databinding.ActivityProductionMaterialCheckBinding
import com.jj.zds.viewmodel.ZdsViewModel

class ProductionMaterialCheckActivity :
    JiangToolbarStateVmActivity<ActivityProductionMaterialCheckBinding, ZdsViewModel>() {

    private val partCode: String by lazy {
        intent.getStringExtra(EXTRA_PART_CODE).orEmpty()
    }

    private val materials: List<ProductionSwitchCategoryDto> by lazy {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        (intent.getSerializableExtra(EXTRA_MATERIAL_LIST) as? ArrayList<ProductionSwitchCategoryDto>).orEmpty()
    }

    override fun createViewBinding(inflater: LayoutInflater): ActivityProductionMaterialCheckBinding {
        return ActivityProductionMaterialCheckBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        renderMaterials()
        binding.btnSubmit.setOnClickListener {
            viewModel.confirmProductionMaterialCheck(materials.firstLineSwiId())
        }
    }

    override fun initPageObserver() {
        viewModel.productionCheckDone.observe(this) {
            JToast.show("物料核对提交成功")
            binding.btnSubmit.visibility = View.GONE
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun toolbarTitle(): CharSequence = partCode.ifBlank { "产品号" }

    override fun toolbarSubtitle(): CharSequence = "物料核对"

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    private fun renderMaterials() {
        binding.layoutMaterials.removeAllViews()
        binding.tvEmpty.visibility = if (materials.isEmpty()) View.VISIBLE else View.GONE
        binding.btnSubmit.visibility = if (materials.isMaterialCheckComplete()) View.GONE else View.VISIBLE
        materials.forEachIndexed { index, item ->
            binding.layoutMaterials.addView(createMaterialView(index + 1, item))
        }
    }

    private fun createMaterialView(index: Int, item: ProductionSwitchCategoryDto): View {
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
                    text = "$index. ${item.categoryName.orDash()}"
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                },
            )
            addView(createInfoLine("料号：${item.categoryId.orDash()}"))
            addView(createInfoLine("物料名称：${item.categoryName.orDash()}"))
            addView(createQuantityLine("用量", item.num.orDash()))
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

    private fun createQuantityLine(label: String, value: String): View {
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
                    text = label
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

    private fun String?.orDash(): String {
        return this?.takeIf { it.isNotBlank() } ?: "-"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    companion object {
        private const val EXTRA_PART_CODE = "extra_part_code"
        private const val EXTRA_MATERIAL_LIST = "extra_material_list"

        fun start(
            context: Context,
            partCode: String,
            materialList: List<ProductionSwitchCategoryDto>,
        ) {
            context.startActivity(
                Intent(context, ProductionMaterialCheckActivity::class.java)
                    .putExtra(EXTRA_PART_CODE, partCode)
                    .putExtra(EXTRA_MATERIAL_LIST, ArrayList(materialList)),
            )
        }
    }
}
