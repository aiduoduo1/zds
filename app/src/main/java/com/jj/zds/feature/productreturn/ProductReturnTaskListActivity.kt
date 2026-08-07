package com.jj.zds.feature.productreturn

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JInputDialog
import com.jj.ui.dialog.JSingleChoiceDialog
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.addDefaultDivider
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.toast.JToast
import com.jj.zds.data.dto.productreturn.AirportDto
import com.jj.zds.data.dto.productreturn.ProductReturnTaskDto
import com.jj.zds.databinding.ActivityProductReturnTaskListBinding
import com.jj.zds.databinding.ItemProductReturnTaskBinding

class ProductReturnTaskListActivity :
    JiangToolbarStateVmActivity<ActivityProductReturnTaskListBinding, ProductReturnTaskListViewModel>() {

    private val adapter = JBindingAdapter(
        inflate = ItemProductReturnTaskBinding::inflate,
        onBind = ::bindTask,
    )
    private val executeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.loadTasks()
    }
    private var waitingAirportDialog = false

    override fun createViewBinding(inflater: LayoutInflater) = ActivityProductReturnTaskListBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerTasks.setupLinear(adapter)
        binding.recyclerTasks.addDefaultDivider()
        binding.refreshLayout.setOnRefreshListener { viewModel.loadTasks() }
        binding.btnCreateTask.setOnClickListener { openCreateDialog() }
    }

    override fun initPageObserver() {
        viewModel.tasks.observe(this) { tasks ->
            binding.refreshLayout.isRefreshing = false
            adapter.submitListWithDiff(tasks)
            binding.emptyTasks.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
            binding.tvTaskSummary.text = "进行中退库任务 ${tasks.count { it.statusLabel != "已结束" }} 单"
        }
        viewModel.taskCreated.observe(this) {
            JToast.show("退库任务创建成功")
            viewModel.loadTasks()
        }
        viewModel.airports.observe(this) { airports ->
            if (waitingAirportDialog && airports.isNotEmpty()) {
                waitingAirportDialog = false
                showCreateDialog(airports)
            }
        }
    }

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        binding.refreshLayout.isRefreshing = false
        super.onUiError(code, message, throwable)
    }

    override fun initData() = viewModel.loadTasks()
    override fun toolbarTitle(): CharSequence = "成品退库"
    override fun toolbarSubtitle(): CharSequence = "创建退库任务后扫描已出库 PRO"

    private fun openCreateDialog() {
        if (viewModel.airports.value.isNullOrEmpty()) {
            waitingAirportDialog = true
            viewModel.loadAirports()
        } else {
            showCreateDialog(viewModel.airports.value.orEmpty())
        }
    }

    private fun showCreateDialog(airports: List<AirportDto>) {
        JSingleChoiceDialog<AirportDto>(this, this)
            .setTitle("创建退库任务")
            .setItems(airports) { it.name.orDash(it.code) }
            .setOnConfirm { airport ->
                val airId = airport?.id.orEmpty()
                if (airId.isBlank()) {
                    JToast.show("请选择机场")
                    return@setOnConfirm
                }
                JInputDialog(this, this)
                    .setTitle("创建退库任务")
                    .setInputTitle("项目名称")
                    .setHint("请输入项目名称")
                    .setOnConfirm { projectName ->
                        val name = projectName.trim()
                        if (name.isBlank()) JToast.show("请输入项目名称")
                        else if (name.length > PROJECT_NAME_MAX_LENGTH) JToast.show("项目名称最多输入 20 个字符")
                        else viewModel.createTask(airId, name)
                    }
                    .show()
            }
            .show()
    }

    private fun bindTask(binding: ItemProductReturnTaskBinding, item: ProductReturnTaskDto, position: Int) {
        binding.tvTaskIndex.text = (position + 1).toString()
        binding.tvTaskCode.text = item.code.orDash(item.id)
        binding.tvTaskStatus.text = item.statusLabel.orDash("待进行")
        binding.tvTaskProject.text = "项目：${item.projectName.orDash()}"
        binding.tvTaskAirport.text = "机场：${item.airportName.orDash()}"
        binding.tvTaskProgress.text = "已退库：${item.returnCount ?: 0} 台"
        binding.tvTaskTime.text = "创建时间：${item.createTime.orDash()}"
        binding.root.setOnClickListener {
            item.id?.takeIf { it.isNotBlank() }?.let { taskId ->
                executeLauncher.launch(
                    ProductReturnExecuteActivity.intent(
                        this,
                        taskId,
                        item.airportName.orEmpty(),
                        item.projectName.orEmpty(),
                    ),
                )
            }
        }
    }

    private fun String?.orDash(vararg fallback: String?) =
        sequenceOf(this, *fallback).firstOrNull { !it.isNullOrBlank() } ?: "-"

    private companion object {
        const val PROJECT_NAME_MAX_LENGTH = 20
    }
}
