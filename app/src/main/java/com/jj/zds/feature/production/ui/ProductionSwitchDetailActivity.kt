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
import com.jj.arch.state.JiangLoadingMode
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.state.JiangErrorMode
import com.jj.zds.R
import com.jj.zds.data.dto.productionplan.ProductionSwitchCategoryDto
import com.jj.zds.data.dto.productionplan.ProductionSwitchCheckProgress
import com.jj.zds.data.dto.productionplan.ProductionSwitchCheckState
import com.jj.zds.data.dto.productionplan.ProductionSwitchDetailDto
import com.jj.zds.data.dto.productionplan.ProductionSwitchEquipmentDto
import com.jj.zds.data.dto.productionplan.ProductionSwitchSoftDto
import com.jj.zds.data.dto.productionplan.ProductionSwitchUserDto
import com.jj.zds.data.dto.productionplan.isAllCheckItemsDone
import com.jj.zds.data.dto.productionplan.requiredEquipmentCount
import com.jj.zds.data.dto.productionplan.toEquipmentCheckProgress
import com.jj.zds.data.dto.productionplan.toPersonCheckProgress
import com.jj.zds.data.dto.productionplan.toStatusCheckProgress
import com.jj.zds.databinding.ActivityProductionSwitchDetailBinding
import com.jj.zds.viewmodel.ZdsViewModel

class ProductionSwitchDetailActivity :
    JiangToolbarStateVmActivity<ActivityProductionSwitchDetailBinding, ZdsViewModel>() {

    private val planId: String by lazy { intent.getStringExtra(EXTRA_PLAN_ID).orEmpty() }
    private var currentDetail: ProductionSwitchDetailDto? = null
    private var selectedType: CheckType? = null

    override fun createViewBinding(inflater: LayoutInflater): ActivityProductionSwitchDetailBinding {
        return ActivityProductionSwitchDetailBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) = Unit

    override fun initPageObserver() {
        viewModel.productionSwitchDetail.observe(this) { detail ->
            currentDetail = detail
            renderDetail(detail)
        }
        viewModel.productionSwitchEnded.observe(this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun initData() {
        viewModel.loadProductionSwitchDetail(planId)
    }

    override fun onResume() {
        super.onResume()
        if (currentDetail != null) {
            selectedType = null
            viewModel.loadProductionSwitchDetail(planId)
        }
    }

    override fun onStateRetryClick() {
        viewModel.loadProductionSwitchDetail(planId)
    }

    override fun toolbarTitle(): CharSequence = "生产前核对"

    override fun toolbarSubtitle(): CharSequence = "产品号：-"

    override fun showBackButton(): Boolean = true

    override fun defaultLoadingMode(): JiangLoadingMode = JiangLoadingMode.PAGE

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    override fun onToolbarBackClick() {
        if (selectedType != null) {
            selectedType = null
            currentDetail?.let { renderDetail(it) }
            return
        }
        super.onToolbarBackClick()
    }

    private fun renderDetail(detail: ProductionSwitchDetailDto) {
        toolbar.setTitleText(selectedType?.title ?: "生产前核对")
        toolbar.setSubtitleText("产品号：${detail.partCode.orDash()}")
        binding.btnEndSwitch.visibility = if (selectedType == null && detail.isAllCheckItemsDone()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.btnEndSwitch.setOnClickListener {
            viewModel.endProductionSwitch(detail.planId.orEmpty())
        }
        when (selectedType) {
            CheckType.EQUIPMENT -> renderEquipmentList(detail.equList)
            CheckType.PERSON -> renderPersonList(detail.userList)
            CheckType.MATERIAL -> renderMaterialList(detail.catList)
            CheckType.SOFTWARE -> renderSoftList(detail.softList)
            null -> renderOverview(detail)
        }
    }

    private fun renderOverview(detail: ProductionSwitchDetailDto) {
        binding.tvSectionTitle.text = "核对项目"
        val checkItems = buildList {
            detail.equList.toCheckItem(CheckType.EQUIPMENT, detail.equList.toEquipmentCheckProgress())?.let { add(it) }
            detail.userList.toCheckItem(CheckType.PERSON, detail.userList.toPersonCheckProgress())?.let { add(it) }
            detail.catList.toCheckItem(CheckType.MATERIAL, detail.catList.toStatusCheckProgress())?.let { add(it) }
            detail.softList.toCheckItem(CheckType.SOFTWARE, detail.softList.toStatusCheckProgress())?.let { add(it) }
        }
        binding.layoutCheckItems.removeAllViews()
        binding.tvEmpty.visibility = if (checkItems.isEmpty()) View.VISIBLE else View.GONE
        checkItems.forEach { item -> binding.layoutCheckItems.addView(createOverviewItemView(item)) }
    }

    private fun renderEquipmentList(items: List<ProductionSwitchEquipmentDto>) {
        renderTypedList(
            title = "设备核对明细",
            emptyText = "暂无设备核对项",
            items = items,
            progressProvider = { listOf(it).toEquipmentCheckProgress() },
            titleProvider = { it.equName.orDash(it.equCode, it.equCatName, it.equCatCode) },
            linesProvider = {
                listOf(
                    "工位：${it.stationCategoryLabel.orDash(it.staName, it.staCode)}",
                    "工位类别：${it.stationTypeLabel.orDash()}",
                    "设备类别：${it.equCatName.orDash(it.equCatCode)}",
                    "设备编号：${it.equCode.orDash()}",
                    "需求数量：${it.requiredEquipmentCount()}",
                    "已核对：${it.equips.size.coerceAtMost(it.requiredEquipmentCount())}/${it.requiredEquipmentCount()}",
                )
            },
        )
    }

    private fun renderPersonList(items: List<ProductionSwitchUserDto>) {
        renderTypedList(
            title = "人员资质核对明细",
            emptyText = "暂无人员资质核对项",
            items = items,
            progressProvider = { listOf(it).toPersonCheckProgress() },
            titleProvider = { it.skiName.orDash(it.skiId, it.userName, it.userId) },
            linesProvider = {
                val checked = it.users.firstOrNull()
                listOf(
                    "工位：${it.staName.orDash(it.staCode)}",
                    "资质：${it.skiName.orDash(it.skiId)}",
                    "已核对：${checked?.userName.orDash(checked?.account, checked?.userId)}",
                )
            },
        )
    }

    private fun renderMaterialList(items: List<ProductionSwitchCategoryDto>) {
        renderTypedList(
            title = "物料核对明细",
            emptyText = "暂无物料核对项",
            items = items,
            progressProvider = { listOf(it).toStatusCheckProgress() },
            titleProvider = { it.categoryName.orDash(it.categoryId) },
            linesProvider = {
                listOf(
                    "物料ID：${it.categoryId.orDash()}",
                    "需求数量：${it.num.orDash()}",
                )
            },
        )
    }

    private fun renderSoftList(items: List<ProductionSwitchSoftDto>) {
        renderTypedList(
            title = "软件版本核对明细",
            emptyText = "暂无软件版本核对项",
            items = items,
            progressProvider = { listOf(it).toStatusCheckProgress() },
            titleProvider = { it.softGrpLabel.orDash(it.softGrp, it.softCode) },
            linesProvider = {
                listOf(
                    "版本类别：${it.softGrpLabel.orDash(it.softGrp)}",
                    "版本代码：${it.softCode.orDash()}",
                )
            },
        )
    }

    private fun <T> renderTypedList(
        title: String,
        emptyText: String,
        items: List<T>,
        progressProvider: (T) -> ProductionSwitchCheckProgress,
        titleProvider: (T) -> String,
        linesProvider: (T) -> List<String>,
    ) {
        binding.tvSectionTitle.text = title
        binding.layoutCheckItems.removeAllViews()
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.tvEmpty.text = emptyText
        items.forEachIndexed { index, item ->
            binding.layoutCheckItems.addView(
                createDetailRowView(
                    index = index + 1,
                    title = titleProvider(item),
                    lines = linesProvider(item),
                    state = progressProvider(item).state.toCheckState(),
                ),
            )
        }
    }

    private fun createBackOverviewButton(): View {
        return TextView(this).apply {
            text = "返回核对项目"
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_primary))
            setBackgroundResource(R.drawable.pda_bg_button_outline)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)).apply {
                bottomMargin = dp(10)
            }
            setOnClickListener {
                selectedType = null
                currentDetail?.let { renderDetail(it) }
            }
        }
    }

    private fun createOverviewItemView(item: CheckItem): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundResource(R.drawable.pda_bg_card)
            elevation = dp(3).toFloat()
            translationZ = dp(1).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(10) }

            addView(createAccentStrip(item.state))
            addView(createCheckTextBlock(item))
            addView(createStatusBadge(item.state))
            setOnClickListener { openCheckItem(item.type) }
        }
    }

    private fun openCheckItem(type: CheckType) {
        val detail = currentDetail ?: return
        when (type) {
            CheckType.EQUIPMENT -> ProductionEquipmentCheckActivity.start(this, detail.partCode.orEmpty(), detail.equList)
            CheckType.MATERIAL -> ProductionMaterialCheckActivity.start(this, detail.partCode.orEmpty(), detail.catList)
            CheckType.PERSON -> ProductionPersonCheckActivity.start(this, detail.partCode.orEmpty(), detail.userList)
            CheckType.SOFTWARE -> ProductionSoftCheckActivity.start(this, detail.partCode.orEmpty(), detail.softList)
        }
    }

    private fun createDetailRowView(index: Int, title: String, lines: List<String>, state: CheckState): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundResource(R.drawable.pda_bg_card)
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(8) }

            addView(createAccentStrip(state))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(
                        TextView(context).apply {
                            text = "$index. $title"
                            textSize = 15f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                        },
                    )
                    lines.filter { it.isNotBlank() }.forEach { line -> addView(createInfoLine(line)) }
                },
            )
            addView(createStateOnlyBadge(state))
        }
    }

    private fun createInfoLine(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(3) }
        }
    }

    private fun createAccentStrip(state: CheckState): View {
        return View(this).apply {
            setBackgroundColor(ContextCompat.getColor(context, state.colorRes))
            layoutParams = LinearLayout.LayoutParams(dp(5), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginEnd = dp(10)
            }
            minimumHeight = dp(54)
        }
    }

    private fun createCheckTextBlock(item: CheckItem): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(
                TextView(context).apply {
                    text = item.type.title
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                },
            )
            addView(
                TextView(context).apply {
                    text = "已核对 ${item.done}/${item.total}"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { topMargin = dp(4) }
                },
            )
        }
    }

    private fun createStatusBadge(state: CheckState): TextView = createStateBadge(state, dp(72), dp(30), dp(10))

    private fun createStateOnlyBadge(state: CheckState): TextView = createStateBadge(state, dp(72), dp(30), dp(8))

    private fun createStateBadge(state: CheckState, minWidthValue: Int, height: Int, marginStartValue: Int): TextView {
        return TextView(this).apply {
            text = state.label
            gravity = Gravity.CENTER
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), 0, dp(8), 0)
            minWidth = minWidthValue
            setTextColor(ContextCompat.getColor(context, state.colorRes))
            setBackgroundResource(state.backgroundRes)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, height).apply {
                marginStart = marginStartValue
            }
        }
    }

    private fun <T> List<T>.toCheckItem(type: CheckType, progress: ProductionSwitchCheckProgress): CheckItem? {
        if (isEmpty()) return null
        return CheckItem(type = type, done = progress.done, total = progress.total, state = progress.state.toCheckState())
    }

    private fun ProductionSwitchCheckState.toCheckState(): CheckState {
        return when (this) {
            ProductionSwitchCheckState.DONE -> CheckState.DONE
            ProductionSwitchCheckState.DOING -> CheckState.DOING
            ProductionSwitchCheckState.WAITING -> CheckState.WAITING
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun String?.orDash(vararg fallback: String?): String {
        return sequenceOf(this, *fallback).firstOrNull { !it.isNullOrBlank() } ?: "-"
    }

    private data class CheckItem(
        val type: CheckType,
        val done: Int,
        val total: Int,
        val state: CheckState,
    )

    private enum class CheckType(val title: String) {
        EQUIPMENT("设备核对"),
        PERSON("人员资质核对"),
        MATERIAL("物料核对"),
        SOFTWARE("软件版本核对"),
    }

    private enum class CheckState(val label: String, val colorRes: Int, val backgroundRes: Int) {
        WAITING("待核对", com.jj.ui.R.color.j_color_warning, R.drawable.pda_bg_status_warning),
        DOING("核对中", com.jj.ui.R.color.j_color_primary, R.drawable.pda_bg_quantity_focus),
        DONE("核对完成", com.jj.ui.R.color.j_color_success, R.drawable.pda_bg_status_success),
    }

    companion object {
        private const val EXTRA_PLAN_ID = "extra_plan_id"
        private const val EXTRA_SWITCH_ID = "extra_switch_id"

        fun start(context: Context, planId: String, switchId: Int) {
            context.startActivity(
                Intent(context, ProductionSwitchDetailActivity::class.java)
                    .putExtra(EXTRA_PLAN_ID, planId)
                    .putExtra(EXTRA_SWITCH_ID, switchId),
            )
        }
    }
}
