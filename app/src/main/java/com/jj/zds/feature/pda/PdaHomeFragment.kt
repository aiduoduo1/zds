package com.jj.zds.feature.pda

import android.content.Context
import android.content.Intent
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
import com.jj.arch.state.JiangUiState
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.data.dto.productionplan.LineDto
import com.jj.zds.data.dto.productionplan.ProductionPlanDto
import com.jj.zds.data.dto.productionplan.ProductionPlanStatus
import com.jj.zds.databinding.FragmentPdaHomeBinding
import com.jj.zds.feature.recheck.ui.RecheckActivity
import com.jj.zds.feature.repair.ui.RepairActivity
import com.jj.zds.feature.singledebug.ui.SingleDebugActivity
import com.jj.zds.viewmodel.ZdsViewModel

class PdaHomeFragment : JiangVmFragment<FragmentPdaHomeBinding, ZdsViewModel>() {

    private var callback: Callback? = null
    private var homeLines = emptyList<LineDto>()
    private var homePlans = emptyList<ProductionPlanDto>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as? Callback
    }

    override fun onDetach() {
        callback = null
        super.onDetach()
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentPdaHomeBinding {
        return FragmentPdaHomeBinding.inflate(inflater, container, false)
    }

    override fun initView(savedInstanceState: Bundle?) {
        renderCurrentProduction(emptyList())
        binding.btnViewProducePlan.setOnClickListener {
            callback?.openProduceTab()
        }
        bindActionPlaceholders()
    }

    override fun initObserver() {
        viewModel.productionLineCounts.observe(viewLifecycleOwner) { lines ->
            homeLines = lines
            renderCurrentProduction(buildHomeLineWorks())
        }
        viewModel.productionPlans.observe(viewLifecycleOwner) { plans ->
            homePlans = plans
            renderCurrentProduction(buildHomeLineWorks())
        }
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state is JiangUiState.Error) {
                JToast.show(state.message)
            }
        }
    }

    override fun initData() {
        refreshCurrentProduction()
    }

    override fun onResume() {
        super.onResume()
        refreshCurrentProduction()
    }

    private fun refreshCurrentProduction() {
        viewModel.loadProductionLineCounts()
        viewModel.loadProductionPlans()
    }

    private fun renderCurrentProduction(lineWorks: List<HomeLineWork>) {
        val hasCurrentPlan = lineWorks.isNotEmpty()
        binding.cardCurrentProduction.visibility = if (hasCurrentPlan) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
        binding.cardNoCurrentProduction.visibility = if (hasCurrentPlan) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
        binding.layoutCurrentLines.removeAllViews()
        lineWorks.forEachIndexed { index, work ->
            binding.layoutCurrentLines.addView(createLineWorkView(work, index))
        }
    }

    private fun createLineWorkView(work: HomeLineWork, index: Int): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.pda_bg_home_current_row_dark)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(66),
            ).apply {
                if (index > 0) topMargin = dp(8)
            }

            addView(
                View(context).apply {
                    setBackgroundColor(ContextCompat.getColor(context, work.colorRes))
                    layoutParams = LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT)
                },
            )

            addView(
                TextView(context).apply {
                    text = work.lineName
                    gravity = Gravity.CENTER
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                    layoutParams = LinearLayout.LayoutParams(dp(58), LinearLayout.LayoutParams.MATCH_PARENT)
                },
            )

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    addView(
                        TextView(context).apply {
                            text = work.planNo
                            textSize = 14f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                            maxLines = 1
                        },
                    )
                    addView(
                        TextView(context).apply {
                            text = work.product
                            textSize = 12f
                            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
                            maxLines = 1
                        },
                    )
                },
            )

            addView(createStatusTag(work))
            addView(createLineAction(work))
            setOnClickListener {
                callback?.openProduceTab(work.lineId)
            }
        }
    }

    private fun createStatusTag(work: HomeLineWork): TextView {
        return TextView(requireContext()).apply {
            text = work.statusText
            gravity = Gravity.CENTER
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, work.colorRes))
            setBackgroundResource(work.statusBgRes)
            layoutParams = LinearLayout.LayoutParams(dp(62), dp(26)).apply {
                marginStart = dp(6)
            }
        }
    }

    private fun createLineAction(work: HomeLineWork): TextView {
        return TextView(requireContext()).apply {
            text = "前往"
            gravity = Gravity.CENTER
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_primary))
            setBackgroundResource(R.drawable.pda_bg_home_go)
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(28)).apply {
                marginStart = dp(6)
                marginEnd = dp(8)
            }
            setOnClickListener {
                callback?.openProduceTab(work.lineId)
            }
        }
    }

    private fun buildHomeLineWorks(): List<HomeLineWork> {
        val eligiblePlans = homePlans.filter { it.planStatus.homePriority() < Int.MAX_VALUE }
        if (eligiblePlans.isEmpty()) return emptyList()

        val lineNameMap = homeLines
            .associateBy(
                keySelector = { it.homeLineKey() },
                valueTransform = { it.lineName },
            )
        val plansByLine = eligiblePlans.groupBy { it.homeLineKey() }
        val orderedKeys = plansByLine.keys
            .filter { it.isNotBlank() }
            .distinct()

        return orderedKeys.mapNotNull { lineKey ->
            val plan = plansByLine[lineKey]
                ?.sortedBy { it.planStatus.homePriority() }
                ?.firstOrNull()
                ?: return@mapNotNull null

            HomeLineWork(
                lineId = plan.lineId,
                lineName = lineNameMap[lineKey].orDash(plan.lineName, "未命名线体"),
                statusText = plan.displayStatus(),
                planNo = plan.orderCode.orDash(plan.id),
                product = buildProductText(plan),
                colorRes = plan.planStatus.statusColorRes(),
                statusBgRes = plan.planStatus.statusBgRes(),
            )
        }
    }

    private fun bindActionPlaceholders() {
        binding.homeActions.btnSingleDebug.setOnClickListener {
            startActivity(Intent(requireContext(), SingleDebugActivity::class.java))
        }
        binding.homeActions.btnRepair.setOnClickListener {
            startActivity(Intent(requireContext(), RepairActivity::class.java))

        }
        binding.homeActions.btnRecheck.setOnClickListener {
            startActivity(Intent(requireContext(), RecheckActivity::class.java))
        }
        binding.homeActions.btnProductIn.setOnClickListener { JToast.show("进入成品入库") }
        binding.homeActions.btnProductOut.setOnClickListener { JToast.show("进入成品出库") }
        binding.homeActions.btnBind.setOnClickListener { JToast.show("进入绑定") }
        binding.homeActions.btnUnbind.setOnClickListener { JToast.show("进入解绑") }
        binding.homeActions.btnMaterialDetail.setOnClickListener { JToast.show("进入材料详情") }
    }

    interface Callback {
        fun openProduceTab(lineId: String? = null)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private data class HomeLineWork(
        val lineId: String?,
        val lineName: String,
        val statusText: String,
        val planNo: String,
        val product: String,
        val colorRes: Int,
        val statusBgRes: Int,
    )

    private fun buildProductText(plan: ProductionPlanDto): String {
        return buildString {
            append(plan.partName.orDash(plan.partCode))
            append("  ")
            append(plan.quantity)
            plan.unitStr?.takeIf { it.isNotBlank() }?.let { append(it) }
        }
    }

    private fun ProductionPlanDto.displayStatus(): String {
        return planStatusName?.takeIf { it.isNotBlank() } ?: when (planStatus) {
            ProductionPlanStatus.PRODUCT_ING -> "生产中"
            ProductionPlanStatus.PAUSE -> "暂停"
            ProductionPlanStatus.WAIT_PRODUCT -> "待生产"
            ProductionPlanStatus.SWITCHING -> "切换中"
            ProductionPlanStatus.WAIT_SWITCH -> "待切换"
            else -> "-"
        }
    }

    private fun Int.homePriority(): Int {
        return when (this) {
            ProductionPlanStatus.PRODUCT_ING -> 0
            ProductionPlanStatus.PAUSE -> 1
            ProductionPlanStatus.WAIT_PRODUCT -> 2
            ProductionPlanStatus.SWITCHING -> 3
            ProductionPlanStatus.WAIT_SWITCH -> 4
            else -> Int.MAX_VALUE
        }
    }

    private fun Int.statusColorRes(): Int {
        return when (this) {
            ProductionPlanStatus.PRODUCT_ING -> com.jj.ui.R.color.j_color_success
            ProductionPlanStatus.PAUSE -> com.jj.ui.R.color.j_color_warning
            ProductionPlanStatus.WAIT_PRODUCT -> com.jj.ui.R.color.j_color_primary
            ProductionPlanStatus.SWITCHING -> com.jj.ui.R.color.j_color_primary
            ProductionPlanStatus.WAIT_SWITCH -> com.jj.ui.R.color.j_color_warning
            else -> com.jj.ui.R.color.j_color_neutral
        }
    }

    private fun Int.statusBgRes(): Int {
        return when (this) {
            ProductionPlanStatus.PRODUCT_ING -> com.jj.ui.R.drawable.j_bg_status_success
            ProductionPlanStatus.PAUSE -> com.jj.ui.R.drawable.j_bg_status_warning
            ProductionPlanStatus.WAIT_PRODUCT -> com.jj.ui.R.drawable.j_bg_status_info
            ProductionPlanStatus.SWITCHING -> com.jj.ui.R.drawable.j_bg_status_info
            ProductionPlanStatus.WAIT_SWITCH -> com.jj.ui.R.drawable.j_bg_status_warning
            else -> com.jj.ui.R.drawable.j_bg_status_neutral
        }
    }

    private fun LineDto.homeLineKey(): String {
        return lineId?.takeIf { it.isNotBlank() } ?: lineName
    }

    private fun ProductionPlanDto.homeLineKey(): String {
        return lineId?.takeIf { it.isNotBlank() } ?: lineName.orEmpty()
    }

    private fun String?.orDash(vararg fallback: String?): String {
        return sequenceOf(this, *fallback).firstOrNull { !it.isNullOrBlank() } ?: "-"
    }
}
