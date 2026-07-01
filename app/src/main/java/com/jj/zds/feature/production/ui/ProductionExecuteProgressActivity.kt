package com.jj.zds.feature.production.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.jj.arch.state.JiangLoadingMode
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.data.dto.productionplan.ProductionStationProgressDto
import com.jj.zds.databinding.ActivityProductionExecuteProgressBinding
import com.jj.zds.databinding.ItemProductionExecuteProgressBinding
import com.jj.zds.viewmodel.ZdsViewModel

class ProductionExecuteProgressActivity :
    JiangToolbarStateVmActivity<ActivityProductionExecuteProgressBinding, ZdsViewModel>() {

    private val planId: String by lazy { intent.getStringExtra(EXTRA_PLAN_ID).orEmpty() }
    private val planNo: String by lazy { intent.getStringExtra(EXTRA_PLAN_NO).orEmpty() }
    private val partCode: String by lazy { intent.getStringExtra(EXTRA_PART_CODE).orEmpty() }

    private val adapter = JBindingAdapter(
        inflate = ItemProductionExecuteProgressBinding::inflate,
        onBind = ::bindProgress,
    )

    override fun createViewBinding(inflater: LayoutInflater): ActivityProductionExecuteProgressBinding {
        return ActivityProductionExecuteProgressBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerProgress.setupLinear(adapter)
        adapter.setOnItemClickListener { item, _ ->
            if (item.operationType == OPERATION_BIND) {
                ProductionSnBindActivity.start(
                    context = this,
                    planId = planId,
                    planNo = planNo,
                )
            }
        }
        binding.tvPlanNo.text = planNo.ifBlank { "生产计划" }
        binding.tvPlanMeta.text = if (partCode.isBlank()) {
            "产品号：-"
        } else {
            "产品号：$partCode"
        }
        renderHeader(emptyList())
    }

    override fun initPageObserver() {
        viewModel.productionStationProgressList.observe(this) { progressList ->
            renderHeader(progressList)
            adapter.setList(progressList)
            binding.tvEmpty.visibility = if (progressList.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun initData() {
        if (planId.isBlank()) {
            JToast.show("生产计划ID为空")
            finish()
            return
        }
        viewModel.loadProductionStationProgressList(planId)
    }

    override fun onStateRetryClick() {
        viewModel.loadProductionStationProgressList(planId)
    }

    override fun toolbarTitle(): CharSequence = "生产执行"

    override fun toolbarSubtitle(): CharSequence = planNo.ifBlank { "生产中计划" }

    override fun showBackButton(): Boolean = true

    override fun defaultLoadingMode(): JiangLoadingMode = JiangLoadingMode.PAGE

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    private fun renderHeader(progressList: List<ProductionStationProgressDto>) {
        val finishedCount = progressList.sumOf { it.finishedCount.coerceAtLeast(0) }
        val totalCount = progressList.sumOf { it.totalCount.coerceAtLeast(0) }
        val percent = percentOf(finishedCount, totalCount)
        binding.tvSummary.text = "执行进度 $finishedCount/$totalCount"
        binding.tvPercent.text = "$percent%"
        binding.progressOverall.progress = percent
    }

    private fun bindProgress(
        itemBinding: ItemProductionExecuteProgressBinding,
        item: ProductionStationProgressDto,
        position: Int,
    ) {
        val total = item.totalCount.coerceAtLeast(0)
        val finished = item.finishedCount.coerceAtLeast(0)
        val safeFinished = if (total > 0) finished.coerceAtMost(total) else finished
        val percent = percentOf(safeFinished, total)

        itemBinding.tvTitle.text = item.operationTitle()
        itemBinding.tvCount.text = "$safeFinished / $total"
        itemBinding.progressBar.progress = percent
        itemBinding.tvPercent.text = "$percent%"
        itemBinding.tvDesc.text = item.progressDesc(safeFinished, total)
        itemBinding.viewStatusStrip.setBackgroundColor(item.statusColor(safeFinished, total))
    }

    private fun ProductionStationProgressDto.operationTitle(): String {
        return when (operationType) {
            OPERATION_BIND -> "扫码绑定"
            OPERATION_TEST -> "整体测试"
            OPERATION_PRINT -> "流转单打印"
            else -> if (operationType > 0) "执行项 $operationType" else "执行项"
        }
    }

    private fun ProductionStationProgressDto.progressDesc(finished: Int, total: Int): String {
        return when {
            total <= 0 -> "暂无目标数量"
            finished >= total -> "已全部完成"
            finished <= 0 -> "待开始，剩余 $total"
            else -> "已完成 $finished，剩余 ${total - finished}"
        }
    }

    private fun percentOf(finished: Int, total: Int): Int {
        if (total <= 0) {
            return 0
        }
        return ((finished * 100f) / total).toInt().coerceIn(0, 100)
    }

    private fun ProductionStationProgressDto.statusColor(finished: Int, total: Int): Int {
        val colorRes = when {
            total > 0 && finished >= total -> com.jj.ui.R.color.j_color_success
            finished > 0 -> com.jj.ui.R.color.j_color_primary
            else -> com.jj.ui.R.color.j_color_warning
        }
        return ContextCompat.getColor(this@ProductionExecuteProgressActivity, colorRes)
    }

    companion object {
        private const val EXTRA_PLAN_ID = "extra_plan_id"
        private const val EXTRA_PLAN_NO = "extra_plan_no"
        private const val EXTRA_PART_CODE = "extra_part_code"

        private const val OPERATION_BIND = 1
        private const val OPERATION_TEST = 2
        private const val OPERATION_PRINT = 3

        fun start(
            context: Context,
            planId: String,
            planNo: String = "",
            partCode: String = "",
        ) {
            context.startActivity(
                Intent(context, ProductionExecuteProgressActivity::class.java)
                    .putExtra(EXTRA_PLAN_ID, planId)
                    .putExtra(EXTRA_PLAN_NO, planNo)
                    .putExtra(EXTRA_PART_CODE, partCode),
            )
        }
    }
}
