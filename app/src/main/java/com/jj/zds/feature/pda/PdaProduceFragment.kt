package com.jj.zds.feature.pda

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jj.arch.base.JiangVmFragment
import com.jj.arch.event.JiangUiEvent
import com.jj.arch.state.JiangUiState
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.data.dto.productionplan.LineDto
import com.jj.zds.data.dto.productionplan.ProductionPlanDto
import com.jj.zds.data.dto.productionplan.ProductionPlanStatus
import com.jj.zds.databinding.FragmentPdaProduceBinding
import com.jj.zds.databinding.ItemPdaProducePlanBinding
import com.jj.zds.feature.production.ui.ProductionExecuteProgressActivity
import com.jj.zds.feature.production.ui.ProductionSwitchDetailActivity
import com.jj.zds.viewmodel.ZdsViewModel

class PdaProduceFragment : JiangVmFragment<FragmentPdaProduceBinding, ZdsViewModel>() {

    private var allPlans = emptyList<ProductionPlanDto>()
    private var lineTabs = emptyList<LineTab>()
    private var selectedLineId: String? = null
    private var pendingSelectedLineId: String? = null
    private var pendingFinishPlanId: String? = null
    private var refreshOnResume = false

    private val adapter = JBindingAdapter(
        inflate = ItemPdaProducePlanBinding::inflate,
        onBind = ::bindPlan,
    )

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentPdaProduceBinding {
        return FragmentPdaProduceBinding.inflate(inflater, container, false)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerProducePlans.setupLinear(adapter)
        adapter.setOnItemClickListener { item, _ ->
            if (item.planStatus == ProductionPlanStatus.PRODUCT_ING) {
                val planId = item.id.orEmpty()
                if (planId.isBlank()) {
                    JToast.show("生产计划ID为空")
                    return@setOnItemClickListener
                }
                refreshOnResume = true
                ProductionExecuteProgressActivity.start(
                    context = requireContext(),
                    planId = planId,
                    planNo = item.orderCode.orEmpty(),
                    partCode = item.partCode.orEmpty(),
                )
            }
        }
        hideSearchTitleRow()
        binding.inputSearch.onInputDone {
            refreshPlans()
        }
        binding.inputSearch.onTextDebounce(delayMillis = 300L) {
            refreshPlans()
        }
        binding.btnRefreshProduce.setOnClickListener {
            setRefreshing(true)
            refreshData()
        }
        binding.refreshLayout.setOnRefreshListener {
            setRefreshing(true)
            refreshData()
        }
        renderLineTabs()
    }

    private fun hideSearchTitleRow() {
        binding.inputSearch.getChildAt(0)?.visibility = View.GONE
        val inputRow = binding.inputSearch.getChildAt(1)
        val params = inputRow?.layoutParams as? LinearLayout.LayoutParams
        params?.let {
            it.topMargin = 0
            inputRow.layoutParams = it
        }
    }

    override fun initObserver() {
        viewModel.productionLineCounts.observe(viewLifecycleOwner) { lines ->
            lineTabs = buildLineTabs(lines)
            val pendingLineId = pendingSelectedLineId
            selectedLineId = if (!pendingLineId.isNullOrBlank()) {
                pendingLineId
            } else if (lineTabs.none { it.lineId == selectedLineId }) {
                lineTabs.firstOrNull()?.lineId
            } else {
                selectedLineId
            }
            pendingSelectedLineId = null
            renderLineTabs()
            renderPlans()
        }
        viewModel.productionPlans.observe(viewLifecycleOwner) { plans ->
            allPlans = plans
            renderPlans()
        }
        viewModel.productionPlansLoadFinished.observe(viewLifecycleOwner) {
            setRefreshing(false)
        }
        viewModel.productionSwitchBeginResult.observe(viewLifecycleOwner) { result ->
            refreshOnResume = true
            ProductionSwitchDetailActivity.start(
                context = requireContext(),
                planId = result.planId,
                switchId = result.switchId,
            )
            refreshData()
        }
        viewModel.productionStartResult.observe(viewLifecycleOwner) { result ->
            val plan = allPlans.firstOrNull { it.id == result.planId }
            refreshOnResume = true
            ProductionExecuteProgressActivity.start(
                context = requireContext(),
                planId = result.planId,
                planNo = plan?.orderCode.orEmpty(),
                partCode = plan?.partCode.orEmpty(),
            )
            refreshData()
        }
        viewModel.productionRunStateChanged.observe(viewLifecycleOwner) { result ->
            JToast.show(if (result.paused) "生产已暂停" else "生产已重新开始")
            refreshData()
        }
        viewModel.productionFinishDone.observe(viewLifecycleOwner) {
            pendingFinishPlanId = null
            JToast.show("计划完成")
            refreshData()
        }
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state is JiangUiState.Error) {
                if (state.code == CODE_FORCE_FINISH_REQUIRED) {
                    showForceFinishDialog(state.message)
                    return@observe
                }
                JToast.show(state.message)
            }
        }
        viewModel.uiEvent.observe(viewLifecycleOwner) { event ->
            val content = event.getContentIfNotHandled()
            if (content is JiangUiEvent.Toast) {
                JToast.show(content.message)
            }
        }
    }

    override fun initData() {
        refreshData()
    }

    override fun onResume() {
        super.onResume()
        if (refreshOnResume) {
            refreshOnResume = false
            refreshData()
        }
    }

    fun selectLine(lineId: String?) {
        val targetLineId = lineId?.trim()?.takeIf { it.isNotBlank() } ?: return
        pendingSelectedLineId = targetLineId
        selectedLineId = targetLineId
        if (isAdded) {
            renderLineTabs()
            renderPlans()
        }
    }

    private fun refreshData() {
        viewModel.loadProductionLineCounts()
        refreshPlans()
    }

    private fun refreshPlans() {
        viewModel.loadProductionPlans(binding.inputSearch.text().trim())
    }

    private fun setRefreshing(refreshing: Boolean) {
        binding.refreshLayout.isRefreshing = refreshing
        binding.btnRefreshProduce.isEnabled = !refreshing
        binding.btnRefreshProduce.alpha = if (refreshing) 0.72f else 1f
    }

    private fun bindPlan(
        itemBinding: ItemPdaProducePlanBinding,
        item: ProductionPlanDto,
        position: Int,
    ) {
        val planNo = item.orderCode?.takeIf { it.isNotBlank() } ?: item.id.orEmpty()
        val planId = item.id.orEmpty()
        itemBinding.tvPlanNo.text = planNo.ifBlank { "-" }
        itemBinding.tvProduct.text = item.partName.orDash()
        itemBinding.tvPartCode.text = "产品号：${item.partCode.orDash()}"
        itemBinding.tvLine.text = "线体：${item.lineName.orDash()}"
        itemBinding.tvQuantity.text = buildString {
            append("计划数量 ").append(item.quantity)
            item.unitStr?.takeIf { it.isNotBlank() }?.let { append(it) }
        }
        itemBinding.tvBatch.text = "批次：${item.lot.orDash()}"
        itemBinding.bindProductionTime(item)
        itemBinding.tvPlanDate.setTextOrGone("业务日期：", item.planDate)
        itemBinding.tvStatus.text = item.displayStatus()
        itemBinding.tvStatus.setStatusStyle(item.planStatus)
        itemBinding.viewStatusStrip.setBackgroundColor(item.planStatus.statusColor())
        bindActionButtons(itemBinding, item.planStatus, planId, planNo)
    }

    private fun ItemPdaProducePlanBinding.bindProductionTime(item: ProductionPlanDto) {
        when (item.planStatus) {
            ProductionPlanStatus.WAIT_SWITCH,
            ProductionPlanStatus.SWITCHING,
            ProductionPlanStatus.WAIT_PRODUCT -> {
                tvStartTime.setTextOrGone("预计开始时间：", item.planStartTime)
                tvActualStartTime.visibility = View.GONE
            }

            ProductionPlanStatus.PRODUCT_ING -> {
                tvStartTime.visibility = View.GONE
                tvActualStartTime.setTextOrGone("实际开始时间：", item.actualStartTime)
            }

            else -> {
                tvStartTime.visibility = View.GONE
                tvActualStartTime.visibility = View.GONE
            }
        }
    }

    private fun bindActionButtons(
        itemBinding: ItemPdaProducePlanBinding,
        status: Int,
        planId: String,
        planNo: String,
    ) {
        itemBinding.btnStartSwitch.visibility = if (
            status == ProductionPlanStatus.WAIT_SWITCH ||
            status == ProductionPlanStatus.SWITCHING
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
        itemBinding.btnStartSwitch.text = if (status == ProductionPlanStatus.SWITCHING) {
            "进入核对"
        } else {
            "开始切换"
        }
        itemBinding.btnStartPlan.visibility = if (status == ProductionPlanStatus.WAIT_PRODUCT) {
            View.VISIBLE
        } else {
            View.GONE
        }
        itemBinding.btnPause.visibility = if (
            status == ProductionPlanStatus.PRODUCT_ING ||
            status == ProductionPlanStatus.PAUSE
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
        itemBinding.btnPause.text = if (status == ProductionPlanStatus.PAUSE) "重新开始" else "暂停"
        itemBinding.btnPause.setBackgroundResource(
            if (status == ProductionPlanStatus.PAUSE) {
                R.drawable.pda_bg_button_primary
            } else {
                R.drawable.pda_bg_button_warning
            },
        )
        itemBinding.btnFinishPlan.visibility = if (
            status == ProductionPlanStatus.PRODUCT_ING ||
            status == ProductionPlanStatus.PAUSE
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }

        itemBinding.btnStartPlan.setOnClickListener {
            viewModel.startProduction(planId)
            return@setOnClickListener
            JToast.show("开始生产：${planNo.ifBlank { "-" }}")
        }
        itemBinding.btnStartSwitch.setOnClickListener {
            if (status == ProductionPlanStatus.SWITCHING) {
                refreshOnResume = true
                ProductionSwitchDetailActivity.start(
                    context = requireContext(),
                    planId = planId,
                    switchId = 0,
                )
            } else {
                viewModel.beginProductionSwitch(planId)
            }
        }
        itemBinding.btnPause.setOnClickListener {
            if (status == ProductionPlanStatus.PAUSE) {
                viewModel.restartProduction(planId)
            } else {
                viewModel.pauseProduction(planId)
            }
        }
        itemBinding.btnFinishPlan.setOnClickListener {
            pendingFinishPlanId = planId
            viewModel.finishProduction(planId)
        }
    }

    private fun showForceFinishDialog(message: String) {
        val planId = pendingFinishPlanId?.takeIf { it.isNotBlank() } ?: return JToast.show(message)
        JConfirmDialog(requireContext(), viewLifecycleOwner)
            .setTitle("强制生产完成")
            .setMessage(message)
            .setConfirmText("确认完成")
            .setOnConfirm {
                viewModel.finishProduction(planId, forceFinish = true)
            }
            .show()
    }

    private fun TextView.setTextOrGone(prefix: String, value: String?) {
        visibility = if (value.isNullOrBlank()) {
            View.GONE
        } else {
            text = "$prefix$value"
            View.VISIBLE
        }
    }

    private fun TextView.setStatusStyle(status: Int) {
        val isRunning = status == ProductionPlanStatus.PRODUCT_ING
        setBackgroundResource(
            if (isRunning) {
                com.jj.ui.R.drawable.j_bg_status_success
            } else {
                com.jj.ui.R.drawable.j_bg_status_warning
            },
        )
        setTextColor(
            ContextCompat.getColor(
                context,
                if (isRunning) com.jj.ui.R.color.j_color_success else com.jj.ui.R.color.j_color_warning,
            ),
        )
    }

    private fun Int.statusColor(): Int {
        val colorRes = when (this) {
            ProductionPlanStatus.PRODUCT_ING -> com.jj.ui.R.color.j_color_success
            ProductionPlanStatus.PAUSE -> com.jj.ui.R.color.j_color_warning
            ProductionPlanStatus.WAIT_SWITCH,
            ProductionPlanStatus.SWITCHING,
            ProductionPlanStatus.WAIT_PRODUCT,
            -> com.jj.ui.R.color.j_color_primary
            else -> com.jj.ui.R.color.j_color_neutral
        }
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun renderPlans() {
        val visiblePlans = allPlans.filter { plan ->
            selectedLineId.isNullOrBlank() || plan.lineId == selectedLineId
        }.sortedWith(compareBy<ProductionPlanDto> { it.lineName.lineSortNo() }.thenBy { it.sort })
        binding.tvProduceEmpty.visibility = if (visiblePlans.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerProducePlans.visibility = if (visiblePlans.isEmpty()) View.GONE else View.VISIBLE
        adapter.setList(visiblePlans)
    }

    private fun buildLineTabs(lines: List<LineDto>): List<LineTab> {
        return lines
            .filter { !it.lineId.isNullOrBlank() }
            .map { line ->
                LineTab(
                    lineId = line.lineId,
                    title = line.lineName.ifBlank { "未命名线体" },
                    count = line.activeCount(),
                )
            }
            .sortedWith(compareBy<LineTab> { it.title.lineSortNo() }.thenBy { it.title })
    }

    private fun String?.lineSortNo(): Int {
        val lineName = this?.trim().orEmpty()
        return Regex("^L\\s*(\\d+)", RegexOption.IGNORE_CASE)
            .find(lineName)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: Int.MAX_VALUE
    }

    private fun renderLineTabs() {
        binding.scrollLineTabs.visibility = if (lineTabs.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutLineTabs.removeAllViews()
        lineTabs.forEach { tab ->
            binding.layoutLineTabs.addView(createLineTabView(tab))
        }
    }

    private fun createLineTabView(tab: LineTab): View {
        val selected = tab.lineId == selectedLineId
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            minimumHeight = dp(36)
            setPadding(dp(10), 0, dp(8), 0)
            setBackgroundResource(
                if (selected) R.drawable.pda_bg_line_tab_selected else R.drawable.pda_bg_line_tab_unselected,
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(36),
            ).apply {
                marginEnd = dp(6)
            }

            addView(createLineNameView(tab.title, selected))
            addView(createLineCountView(tab.count, selected))
            setOnClickListener {
                if (selectedLineId == tab.lineId) return@setOnClickListener
                selectedLineId = tab.lineId
                renderLineTabs()
                renderPlans()
            }
        }
    }

    private fun createLineNameView(title: String, selected: Boolean): TextView {
        return TextView(requireContext()).apply {
            text = title
            gravity = Gravity.CENTER
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) com.jj.ui.R.color.j_color_text_primary else com.jj.ui.R.color.j_color_text_secondary,
                ),
            )
        }
    }

    private fun createLineCountView(count: Int, selected: Boolean): TextView {
        return TextView(requireContext()).apply {
            text = count.toString()
            gravity = Gravity.CENTER
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), 0, 0, 0)
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) com.jj.ui.R.color.j_color_primary else com.jj.ui.R.color.j_color_text_hint,
                ),
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = dp(8)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun LineDto.activeCount(): Int {
        return toSwitch + inSwitch + toProduce + inProduce + stop
    }

    private fun ProductionPlanDto.displayStatus(): String {
        return planStatusName?.takeIf { it.isNotBlank() } ?: when (planStatus) {
            ProductionPlanStatus.WAIT_SWITCH -> "待切换"
            ProductionPlanStatus.SWITCHING -> "切换中"
            ProductionPlanStatus.WAIT_PRODUCT -> "待生产"
            ProductionPlanStatus.PRODUCT_ING -> "生产中"
            ProductionPlanStatus.PAUSE -> "暂停"
            else -> "-"
        }
    }

    private fun String?.orDash(): String {
        return this?.takeIf { it.isNotBlank() } ?: "-"
    }

    private data class LineTab(
        val lineId: String?,
        val title: String,
        val count: Int,
    )

    companion object {
        private const val CODE_FORCE_FINISH_REQUIRED = 10087
    }
}
