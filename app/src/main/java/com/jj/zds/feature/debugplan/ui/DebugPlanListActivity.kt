package com.jj.zds.feature.debugplan.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.recycler.page.JRecyclerPageHelper
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toolbar.JiangToolbarMenuItem
import com.jj.ui.widget.JStatusType
import com.jj.zds.data.dto.debugplan.DebugPlanDto
import com.jj.zds.data.dto.debugplan.DebugPlanStatisticsDto
import com.jj.zds.data.dto.debugplan.DebugPlanStatus
import com.jj.zds.databinding.ActivityDebugPlanListBinding
import com.jj.zds.databinding.ItemDebugPlanBinding
import com.jj.zds.viewmodel.ZdsViewModel

class DebugPlanListActivity : JiangToolbarStateVmActivity<ActivityDebugPlanListBinding, ZdsViewModel>() {

    private var plans = emptyList<DebugPlanDto>()
    private var statistics = DebugPlanStatisticsDto()
    private var currentFilter = PlanFilter.ALL
    private var refreshOnResume = false
    private lateinit var pageHelper: JRecyclerPageHelper<DebugPlanDto, ItemDebugPlanBinding>

    private val adapter = JBindingAdapter(
        inflate = ItemDebugPlanBinding::inflate,
        onBind = ::bindPlan,
    )

    override fun createViewBinding(inflater: LayoutInflater): ActivityDebugPlanListBinding {
        return ActivityDebugPlanListBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerPlans.setupLinear(adapter)
        binding.recyclerPlans.addDefaultDivider()
        pageHelper = JRecyclerPageHelper(
            recyclerView = binding.recyclerPlans,
            adapter = adapter,
            emptyView = binding.emptyView,
            refreshLayout = binding.refreshLayout,
        )
        binding.emptyView.setAction("重置筛选") {
            binding.inputSearch.setText("")
            currentFilter = PlanFilter.ALL
            refreshPlans()
        }
        binding.refreshLayout.setOnRefreshListener {
            binding.inputSearch.setText("")
            currentFilter = PlanFilter.ALL
            refreshPlans()
        }
        adapter.setOnItemClickListener { item, _ ->
            refreshOnResume = true
            DebugPlanBatchActivity.start(
                context = this,
                taskId = item.id,
                requireSubmitPhoto = item.requireSubmitPhoto,
            )
        }

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

    override fun onResume() {
        super.onResume()
        if (refreshOnResume) {
            refreshOnResume = false
            refreshPlans()
        }
    }

    override fun initPageObserver() {
        viewModel.debugPlans.observe(this) { result ->
            plans = result
            pageHelper.setRefreshing(false)
            renderPlans()
        }
        viewModel.debugPlanStatistics.observe(this) { result ->
            statistics = result
            renderCounts()
        }
    }

    override fun toolbarTitle(): CharSequence = "调试计划"

    override fun toolbarSubtitle(): CharSequence = "请选择调试计划"

    override fun toolbarMenuItems(): List<JiangToolbarMenuItem> {
        return listOf(JiangToolbarMenuItem(MENU_SYNC, "刷新"))
    }

    override fun onToolbarMenuClick(item: JiangToolbarMenuItem) {
        if (item.id == MENU_SYNC) {
            binding.inputSearch.setText("")
            currentFilter = PlanFilter.ALL
            refreshPlans()
        }
    }

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        pageHelper.setRefreshing(false)
        super.onUiError(code, message, throwable)
    }

    /**
     * 绑定单个调试计划卡片。这里不做状态过滤，只负责把 DTO 展示成列表行。
     */
    private fun bindPlan(
        itemBinding: ItemDebugPlanBinding,
        item: DebugPlanDto,
        position: Int,
    ) {
        itemBinding.tvPlanNo.text = item.code.ifBlank { item.id }
        itemBinding.tvMaterial.text = item.material.ifBlank { "-" }
        itemBinding.tvMaterialCode.text = buildString {
            append("编码：").append(item.materialCode.ifBlank { "-" })
            item.specification?.takeIf { it.isNotBlank() }?.let {
                append(" / 规格：").append(it)
            }
        }
        itemBinding.tvCustomer.text = "供应商：${item.customer.ifBlank { "-" }}"
        itemBinding.tvBatch.text = "执行批次：${item.batchText}"
        itemBinding.tvPlanQty.text = item.planQty.toString()
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

    /**
     * 列表展示层过滤：隐藏已完成计划，并叠加搜索关键字和顶部状态筛选。
     */
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

    /**
     * 本地搜索只匹配计划号、计划 id 和物料编码，避免扫出来的列表过宽。
     */
    private fun DebugPlanDto.matchesSearchKeyword(keyword: String): Boolean {
        if (keyword.isBlank()) return true
        val text = buildString {
            append(code).append(' ')
            append(id).append(' ')
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
        binding.btnFilterAll.setSelectedFilter(currentFilter == PlanFilter.ALL)
        binding.btnFilterTodo.setSelectedFilter(currentFilter == PlanFilter.TODO)
        binding.btnFilterDoing.setSelectedFilter(currentFilter == PlanFilter.DOING)
    }

    /**
     * 同步按钮和下拉刷新共用入口：统计数量和列表数据分别刷新。
     */
    private fun refreshPlans() {
        pageHelper.setRefreshing(true)
        renderFilterButtons()
        viewModel.loadDebugPlanStatistics()
        viewModel.loadDebugPlans(status = currentFilter.status)
    }

    private fun TextView.setSelectedFilter(selected: Boolean) {
        isSelected = selected
        setTextColor(
            getColor(
                if (selected) {
                    com.jj.ui.R.color.j_color_text_white
                } else {
                    com.jj.ui.R.color.j_color_primary
                },
            ),
        )
        setBackgroundResource(
            if (selected) {
                com.jj.ui.R.drawable.j_bg_button_primary
            } else {
                com.jj.ui.R.drawable.j_bg_button_secondary
            },
        )
    }

    private fun DebugPlanStatus.toTagType(): JStatusType {
        return when (this) {
            DebugPlanStatus.TODO -> JStatusType.WARNING
            DebugPlanStatus.DOING -> JStatusType.INFO
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

    private companion object {
        const val MENU_SYNC = 1
    }
}
