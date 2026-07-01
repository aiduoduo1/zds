package com.jj.zds.feature.repair.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.widget.JStatusType
import com.jj.zds.data.dto.repair.RepairLogDto
import com.jj.zds.data.dto.repair.RepairSnScanValidDto
import com.jj.zds.databinding.ActivityRepairHistoryBinding
import com.jj.zds.databinding.ItemRepairTimelineBinding
import com.jj.zds.viewmodel.ZdsViewModel

class RepairHistoryActivity : JiangToolbarStateVmActivity<ActivityRepairHistoryBinding, ZdsViewModel>() {

    private var infoId: Long = 0L

    override fun createViewBinding(inflater: LayoutInflater): ActivityRepairHistoryBinding {
        return ActivityRepairHistoryBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        infoId = intent.getLongExtra(EXTRA_INFO_ID, 0L)
        val status = intent.getStringExtra(EXTRA_STATUS).orDash()
        binding.tvSn.text = intent.getStringExtra(EXTRA_SN).orDash()
        binding.tagStatus.setStatus(status, status.toStatusType())
        binding.tvEmpty.text = "处理轨迹加载中..."
        binding.tvEmpty.visibility = View.VISIBLE
        viewModel.loadRepairLogs(infoId)
    }

    override fun initPageObserver() {
        viewModel.repairLogs.observe(this) { logs ->
            renderLogs(logs)
        }
    }

    override fun toolbarTitle(): CharSequence = "处理记录"

    override fun toolbarSubtitle(): CharSequence = intent.getStringExtra(EXTRA_SN).orDash()

    override fun showBackButton(): Boolean = true

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    private fun renderLogs(logs: List<RepairLogDto>) {
        binding.layoutLogs.removeAllViews()
        binding.tvEmpty.text = "暂无处理轨迹"
        binding.tvEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        logs.forEachIndexed { index, log ->
            val item = ItemRepairTimelineBinding.inflate(layoutInflater, binding.layoutLogs, false)
            val display = RepairTimelineDisplay.parse(log.opDesc)
            item.tvOperation.text = log.opName.orDash()
            item.tvTime.text = "时间：${log.opTime.orDash()}"
            item.tvOperator.text = "操作人：${(log.createByName ?: log.opBy).orDash()}"
            item.tvContent.text = display.content.ifBlank { "-" }
            item.tagStatus.visibility = if (display.status.isBlank()) View.GONE else View.VISIBLE
            if (display.status.isNotBlank()) {
                item.tagStatus.setStatus(display.status, display.status.toStatusType())
            }
            item.viewLine.visibility = if (index == logs.lastIndex) View.INVISIBLE else View.VISIBLE
            binding.layoutLogs.addView(item.root)
        }
    }

    private fun String?.orDash(): String = takeIf { !it.isNullOrBlank() } ?: "-"

    private fun String.toStatusType(): JStatusType {
        return when {
            contains("待维修") -> JStatusType.WARNING
            contains("完成") || contains("通过") || equals("OK", ignoreCase = true) -> JStatusType.SUCCESS
            contains("维修中") || contains("复调") -> JStatusType.INFO
            else -> JStatusType.NEUTRAL
        }
    }

    companion object {
        private const val EXTRA_INFO_ID = "extra_info_id"
        private const val EXTRA_SN = "extra_sn"
        private const val EXTRA_STATUS = "extra_status"

        fun start(context: Context, report: RepairSnScanValidDto) {
            context.startActivity(
                Intent(context, RepairHistoryActivity::class.java)
                    .putExtra(EXTRA_INFO_ID, report.id?.toLongOrNull() ?: 0L)
                    .putExtra(EXTRA_SN, report.sn)
                    .putExtra(EXTRA_STATUS, report.statusName),
            )
        }
    }
}
