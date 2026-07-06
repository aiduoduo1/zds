package com.jj.zds.feature.debugplan.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.jj.arch.base.JiangVmFragment
import com.jj.arch.event.JiangUiEvent
import com.jj.arch.state.JiangUiState
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.recycler.page.JRecyclerPageHelper
import com.jj.ui.toast.JToast
import com.jj.ui.widget.JStatusType
import com.jj.zds.R
import com.jj.zds.data.dto.debugplan.DebugPlanDto
import com.jj.zds.data.dto.debugplan.DebugPlanStatisticsDto
import com.jj.zds.data.dto.debugplan.DebugPlanStatus
import com.jj.zds.databinding.ActivityDebugPlanListBinding
import com.jj.zds.databinding.ItemDebugPlanBinding
import com.jj.zds.viewmodel.ZdsViewModel

class DebugPlanListFragment : JiangVmFragment<ActivityDebugPlanListBinding, ZdsViewModel>() {

    private var plans = emptyList<DebugPlanDto>()
    private var statistics = DebugPlanStatisticsDto()
    private var currentFilter = PlanFilter.ALL
    private var refreshOnResume = false
    private lateinit var pageHelper: JRecyclerPageHelper<DebugPlanDto, ItemDebugPlanBinding>

    private val adapter = JBindingAdapter(
        inflate = ItemDebugPlanBinding::inflate,
        onBind = ::bindPlan,
    )

    private val batchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshPlans()
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): ActivityDebugPlanListBinding {
        return ActivityDebugPlanListBinding.inflate(inflater, container, false)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerPlans.setupLinear(adapter)
        binding.recyclerPlans.addDefaultDivider()
        hideSearchTitleRow()
        pageHelper = JRecyclerPageHelper(
            recyclerView = binding.recyclerPlans,
            adapter = adapter,
            emptyView = binding.emptyView,
            refreshLayout = binding.refreshLayout,
        )
        binding.emptyView.setAction("重置筛选") {
            resetAndRefresh()
        }
        binding.refreshLayout.setOnRefreshListener {
            resetAndRefresh()
        }
        adapter.setOnItemClickListener { item, _ ->
            refreshOnResume = true
            batchLauncher.launch(
                DebugPlanBatchActivity.createIntent(
                    context = requireContext(),
                    taskId = item.id,
                    requireSubmitPhoto = item.requireSubmitPhoto,
                    planMaterialCode = item.materialCode,
                    planCode = item.code,
                    bindSnCount = item.bindSnCount,
                    planCheckQty = item.planCheckQty,
                ),
            )
        }
        binding.inputSearch.requestFocus()

        binding.inputSearch.onTextChanged {
            renderPlans()
        }
        binding.inputSearch.onInputDone {
            renderPlans()
        }
        binding.btnFilterAll.setOnClickListener { changeFilter(PlanFilter.ALL) }
        binding.btnFilterTodo.setOnClickListener { changeFilter(PlanFilter.TODO) }
        binding.btnFilterDoing.setOnClickListener { changeFilter(PlanFilter.DOING) }

        refreshPlans()
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
        viewModel.debugPlans.observe(viewLifecycleOwner) { result ->
            plans = result
            renderPlans()
        }
        viewModel.debugPlansLoadFinished.observe(viewLifecycleOwner) {
            pageHelper.setRefreshing(false)
        }
        viewModel.debugPlanStatistics.observe(viewLifecycleOwner) { result ->
            statistics = result
            renderCounts()
        }
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state is JiangUiState.Error) {
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

    override fun onResume() {
        super.onResume()
        if (refreshOnResume) {
            refreshOnResume = false
            refreshPlans()
        }
    }

    private fun resetAndRefresh() {
        binding.inputSearch.setText("")
        currentFilter = PlanFilter.ALL
        refreshPlans()
    }

    private fun bindPlan(
        itemBinding: ItemDebugPlanBinding,
        item: DebugPlanDto,
        position: Int,
    ) {
        itemBinding.tvPlanNo.text = item.code.ifBlank { item.id }
        itemBinding.tvMaterial.text = item.material.ifBlank { "-" }
        itemBinding.tvMaterialCode.text = buildString {
            append("物料号：").append(item.materialCode.ifBlank { "-" })
            item.specification?.takeIf { it.isNotBlank() }?.let {
                append(" 规格：").append(it)
            }
        }
        itemBinding.tvCustomer.text = "供应商：${item.customer.ifBlank { "-" }}"
        itemBinding.tvBatch.text = "执行批次：${item.batchText}"
        itemBinding.tvPlanQty.text = "计划数量 ${item.planQty}"
        itemBinding.tvSample.text = "累计已检 / 应检：${item.submittedBatchCount} / ≥${item.requiredSamples}"
        itemBinding.tvNg.text = "累计不良 / 上限：${item.ngCount} / ≤${item.ngLimit}"
        itemBinding.tvEnter.text = "进入执行"
        itemBinding.tagStatus.setStatus(item.statusName ?: item.planStatus.displayName, item.planStatus.toTagType())
        itemBinding.tagPhoto.visibility = if (item.requireSubmitPhoto) View.VISIBLE else View.GONE
    }

    private fun changeFilter(filter: PlanFilter) {
        if (currentFilter == filter) return
        currentFilter = filter
        refreshPlans()
    }

    private fun renderPlans() {
        renderCounts()
        renderFilterButtons()

        val keyword = binding.inputSearch.text()
        val visiblePlans = plans.filter { plan ->
            plan.planStatus != DebugPlanStatus.DONE &&
                plan.matchesSearchKeyword(keyword) &&
                currentFilter.matches(plan)
        }
        pageHelper.showData(visiblePlans)
    }

    private fun DebugPlanDto.matchesSearchKeyword(keyword: String): Boolean {
        if (keyword.isBlank()) return true
        val text = buildString {
            append(code).append(' ')
            append(materialCode)
        }
        return text.contains(keyword, ignoreCase = true)
    }

    private fun renderCounts() {
        binding.btnFilterAll.text = "全部 ${statistics.dzxCount + statistics.zxzCount}"
        binding.btnFilterTodo.text = "待调试 ${statistics.dzxCount}"
        binding.btnFilterDoing.text = "调试中 ${statistics.zxzCount}"
    }

    private fun renderFilterButtons() {
        binding.btnFilterAll.setSelectedFilter(PlanFilter.ALL, currentFilter == PlanFilter.ALL)
        binding.btnFilterTodo.setSelectedFilter(PlanFilter.TODO, currentFilter == PlanFilter.TODO)
        binding.btnFilterDoing.setSelectedFilter(PlanFilter.DOING, currentFilter == PlanFilter.DOING)
    }

    private fun refreshPlans() {
        if (!::pageHelper.isInitialized) return
        pageHelper.setRefreshing(true)
        renderFilterButtons()
        viewModel.loadDebugPlanStatistics()
        viewModel.loadDebugPlans(status = currentFilter.status)
    }

    private fun TextView.setSelectedFilter(filter: PlanFilter, selected: Boolean) {
        isSelected = selected
        setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) filter.selectedTextColor() else com.jj.ui.R.color.j_color_text_primary,
            ),
        )
        setBackgroundResource(if (selected) filter.selectedBackground() else R.drawable.pda_bg_filter_unselected)
    }

    private fun PlanFilter.selectedBackground(): Int {
        return when (this) {
            PlanFilter.ALL -> R.drawable.pda_bg_filter_all_selected
            PlanFilter.TODO -> R.drawable.pda_bg_filter_todo_selected
            PlanFilter.DOING -> R.drawable.pda_bg_filter_doing_selected
        }
    }

    private fun PlanFilter.selectedTextColor(): Int {
        return when (this) {
            PlanFilter.ALL -> com.jj.ui.R.color.j_color_primary
            PlanFilter.TODO -> com.jj.ui.R.color.j_color_warning
            PlanFilter.DOING -> com.jj.ui.R.color.j_color_success
        }
    }

    private fun DebugPlanStatus.toTagType(): JStatusType {
        return when (this) {
            DebugPlanStatus.TODO -> JStatusType.WARNING
            DebugPlanStatus.DOING -> JStatusType.SUCCESS
            DebugPlanStatus.DONE -> JStatusType.SUCCESS
        }
    }

    private enum class PlanFilter(
        val status: Int?,
    ) {
        ALL(status = null),
        TODO(status = 1),
        DOING(status = 2),
        ;

        fun matches(plan: DebugPlanDto): Boolean {
            return when (this) {
                ALL -> true
                TODO -> plan.planStatus == DebugPlanStatus.TODO
                DOING -> plan.planStatus == DebugPlanStatus.DOING
            }
        }
    }
}
