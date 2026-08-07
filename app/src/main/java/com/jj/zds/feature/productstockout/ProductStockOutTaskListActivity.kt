package com.jj.zds.feature.productstockout

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.zds.data.dto.productstockout.ProductStockOutTaskDto
import com.jj.zds.databinding.ActivityProductStockOutTaskListBinding
import com.jj.zds.databinding.ItemProductStockOutTaskBinding

class ProductStockOutTaskListActivity :
    JiangToolbarStateVmActivity<ActivityProductStockOutTaskListBinding, ProductStockOutTaskListViewModel>() {

    private val adapter = JBindingAdapter(
        inflate = ItemProductStockOutTaskBinding::inflate,
        onBind = ::bindTask,
    )
    private val childTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) viewModel.loadTasks()
    }

    override fun createViewBinding(inflater: LayoutInflater) =
        ActivityProductStockOutTaskListBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerTasks.setupLinear(adapter)
        binding.recyclerTasks.addDefaultDivider()
        binding.refreshLayout.setOnRefreshListener { viewModel.loadTasks() }
    }

    override fun initPageObserver() {
        viewModel.tasks.observe(this) { tasks ->
            binding.refreshLayout.isRefreshing = false
            adapter.submitListWithDiff(tasks)
            binding.emptyTasks.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
            binding.tvTaskSummary.text = "待执行出库任务 ${tasks.size} 单"
        }
    }

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        binding.refreshLayout.isRefreshing = false
        super.onUiError(code, message, throwable)
    }

    override fun initData() {
        viewModel.loadTasks()
    }

    override fun toolbarTitle(): CharSequence = "成品出库"

    override fun toolbarSubtitle(): CharSequence = "选择已下达的出货任务"

    private fun bindTask(
        itemBinding: ItemProductStockOutTaskBinding,
        item: ProductStockOutTaskDto,
        position: Int,
    ) {
        itemBinding.tvTaskIndex.text = (position + 1).toString()
        itemBinding.tvTaskCode.text = item.code.orDash(item.id)
        itemBinding.tvTaskStatus.text = item.statusLabel.orDash("待执行")
        itemBinding.tvTaskProject.text = "项目：${item.projectCode.orDash()}"
        itemBinding.tvTaskAirport.text = "机场：${item.airportName.orDash(item.airportCode)}"
        itemBinding.tvTaskProgress.text = "出库进度：${item.finishedQty ?: 0} / ${item.totalQty ?: 0} 台"
        itemBinding.tvTaskDeliveryTime.text = "计划发货：${item.deliveryTime.orDash()}"
        itemBinding.root.setOnClickListener {
            val taskId = item.id.orEmpty()
            if (taskId.isNotBlank()) {
                childTaskLauncher.launch(
                    ProductStockOutChildTaskListActivity.intent(this, taskId, item.code.orEmpty()),
                )
            }
        }
    }

    private fun String?.orDash(vararg fallback: String?): String {
        return sequenceOf(this, *fallback).firstOrNull { !it.isNullOrBlank() } ?: "-"
    }
}
