package com.jj.zds.feature.productstockout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.zds.data.dto.productstockout.ProductStockOutChildTaskDto
import com.jj.zds.databinding.ActivityProductStockOutChildTaskListBinding
import com.jj.zds.databinding.ItemProductStockOutChildTaskBinding

class ProductStockOutChildTaskListActivity :
    JiangToolbarStateVmActivity<ActivityProductStockOutChildTaskListBinding, ProductStockOutChildTaskListViewModel>() {

    private val adapter = JBindingAdapter(
        inflate = ItemProductStockOutChildTaskBinding::inflate,
        onBind = ::bindChildTask,
    )

    private val taskId: String by lazy { intent.getStringExtra(EXTRA_TASK_ID).orEmpty() }
    private val taskCode: String by lazy { intent.getStringExtra(EXTRA_TASK_CODE).orEmpty() }
    private val executeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.loadChildTasks(taskId)
    }

    override fun createViewBinding(inflater: LayoutInflater) =
        ActivityProductStockOutChildTaskListBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerChildTasks.setupLinear(adapter)
        binding.recyclerChildTasks.addDefaultDivider()
        binding.tvTaskCode.text = "出货任务：${taskCode.ifBlank { taskId }}"
        binding.btnFinishTask.setOnClickListener { confirmFinishTask() }
        binding.refreshLayout.setOnRefreshListener { viewModel.loadChildTasks(taskId) }
    }

    override fun initPageObserver() {
        viewModel.childTasks.observe(this) { childTasks ->
            binding.refreshLayout.isRefreshing = false
            adapter.submitListWithDiff(childTasks)
            binding.emptyChildTasks.visibility = if (childTasks.isEmpty()) View.VISIBLE else View.GONE
            binding.tvChildTaskSummary.text = "出库子任务 ${childTasks.size} 项"
        }
        viewModel.taskFinished.observe(this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        binding.refreshLayout.isRefreshing = false
        super.onUiError(code, message, throwable)
    }

    override fun initData() {
        if (taskId.isBlank()) {
            finish()
        } else {
            viewModel.loadChildTasks(taskId)
        }
    }

    override fun toolbarTitle(): CharSequence = "出库子任务"

    override fun toolbarSubtitle(): CharSequence = "请选择需执行的产品任务"

    private fun bindChildTask(
        itemBinding: ItemProductStockOutChildTaskBinding,
        item: ProductStockOutChildTaskDto,
        position: Int,
    ) {
        itemBinding.tvChildTaskIndex.text = (position + 1).toString()
        itemBinding.tvChildTaskName.text = item.productName.orDash(item.categoryName)
        itemBinding.tvChildTaskStatus.text = item.statusLabel.orDash("待出库")
        itemBinding.tvChildTaskSeries.text = listOfNotNull(
            item.seriesName?.takeIf { it.isNotBlank() },
            item.specification?.takeIf { it.isNotBlank() },
        ).joinToString(" / ").ifBlank { "规格：-" }
        itemBinding.tvChildTaskWarehouse.text = "仓库：${item.warehouseName.orDash(item.warehouseCode)}"
        itemBinding.tvChildTaskQuantity.text = "需求 ${item.reqQuantity ?: 0} 台 · 待出库 ${item.deliveryQuantity ?: 0} 台"
        itemBinding.root.setOnClickListener {
            item.id?.takeIf { it.isNotBlank() }?.let { detailId ->
                executeLauncher.launch(ProductStockOutExecuteActivity.intent(this, detailId, item.productName.orDash(item.categoryName)))
            }
        }
    }

    private fun confirmFinishTask() {
        JConfirmDialog(this, this)
            .setTitle("结束出货任务？")
            .setMessage("结束后将无法继续出库，请确认所有子任务已完成。")
            .setConfirmText("确认结束")
            .setOnConfirm { viewModel.finishTask(taskId) }
            .show()
    }

    private fun String?.orDash(vararg fallback: String?): String {
        return sequenceOf(this, *fallback).firstOrNull { !it.isNullOrBlank() } ?: "-"
    }

    companion object {
        private const val EXTRA_TASK_ID = "extra_task_id"
        private const val EXTRA_TASK_CODE = "extra_task_code"

        fun intent(context: Context, taskId: String, taskCode: String): Intent {
            return Intent(context, ProductStockOutChildTaskListActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_TASK_CODE, taskCode)
        }
    }
}
