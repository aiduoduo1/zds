package com.jj.zds.feature.repair.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import coil.load
import com.jj.storage.JiangStorage
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.state.JiangErrorMode
import com.jj.zds.data.local.ZdsStorageKeys
import com.jj.zds.data.dto.repair.RepairLogDto
import com.jj.zds.data.dto.repair.RepairSnScanValidDto
import com.jj.zds.databinding.ActivityRepairHistoryBinding
import com.jj.zds.feature.repair.data.buildRepairFileUrl
import com.jj.zds.viewmodel.ZdsViewModel

class RepairHistoryActivity : JiangToolbarStateVmActivity<ActivityRepairHistoryBinding, ZdsViewModel>() {

    private var infoId: Long = 0L

    override fun createViewBinding(inflater: LayoutInflater): ActivityRepairHistoryBinding {
        return ActivityRepairHistoryBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        infoId = intent.getLongExtra(EXTRA_INFO_ID, 0L)
        renderReportSummary()
        renderCurrentRepair()
        renderFiles()
        binding.tvRepair.text = "处理轨迹加载中..."
        viewModel.loadRepairLogs(infoId)
    }

    override fun initPageObserver() {
        viewModel.repairLogs.observe(this) { logs ->
            renderLogs(logs)
        }
    }

    override fun toolbarTitle(): CharSequence = "处理记录"

    override fun toolbarSubtitle(): CharSequence = intent.getStringExtra(EXTRA_SN).orDash()

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    private fun renderReportSummary() {
        binding.tvReport.text = buildString {
            appendLine("不良上报")
            appendLine("SN：${intent.getStringExtra(EXTRA_SN).orDash()}")
            appendLine("来源：${intent.getStringExtra(EXTRA_SOURCE).orDash()}")
            appendLine("单号：${intent.getStringExtra(EXTRA_PLAN).orDash()}")
            appendLine("批次：${intent.getStringExtra(EXTRA_BATCH).orDash()}")
            appendLine("现象：${intent.getStringExtra(EXTRA_NG_PHENOMENA).orDash()}")
            appendLine("说明：${intent.getStringExtra(EXTRA_NG_REMARK).orDash()}")
            appendLine("上报人：${intent.getStringExtra(EXTRA_USER).orDash()}")
            append("上报时间：${intent.getStringExtra(EXTRA_CREATE_TIME).orDash()}")
        }
    }

    private fun renderCurrentRepair() {
        binding.tvRepair.text = buildString {
            appendLine("当前维修信息")
            appendLine("状态：${intent.getStringExtra(EXTRA_STATUS).orDash()}")
            appendLine("结果：${intent.getStringExtra(EXTRA_RESULT).orDash()}")
            appendLine("原因：${intent.getStringExtra(EXTRA_NG_REASON).orDash()}")
            appendLine("维修人：${intent.getStringExtra(EXTRA_REPAIR_BY).orDash()}")
            append("说明：${intent.getStringExtra(EXTRA_REPAIR_DESC).orDash()}")
        }
    }

    private fun renderFiles() {
        val files = intent.getStringArrayListExtra(EXTRA_FILES).orEmpty()
            .filter { it.isNotBlank() }
        binding.tvFiles.text = buildString {
            append("维修照片：").append(files.size).append(" 张")
        }
        binding.layoutPhotoList.removeAllViews()
        binding.scrollPhotoList.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
        files.forEach { fileId ->
            val url = buildRepairFileUrl(fileId, currentToken()) ?: return@forEach
            binding.layoutPhotoList.addView(createPhotoView(url, fileId))
        }
    }

    private fun renderLogs(logs: List<RepairLogDto>) {
        binding.tvRepair.text = buildString {
            appendLine("处理轨迹")
            if (logs.isEmpty()) {
                append("暂无处理轨迹")
            } else {
                logs.forEachIndexed { index, log ->
                    if (index > 0) appendLine()
                    appendLine("${index + 1}. ${log.opName.orDash()}")
                    appendLine("时间：${log.opTime.orDash()}")
                    appendLine("操作人：${log.opBy.orDash()}")
                    append("内容：${log.opDesc.orDash()}")
                    if (index != logs.lastIndex) appendLine()
                }
            }
        }
    }

    private fun String?.orDash(): String = takeIf { !it.isNullOrBlank() } ?: "-"

    private fun createPhotoView(url: String, fileId: String): ImageView {
        return ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(92), dp(92)).apply {
                marginEnd = dp(8)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "维修照片 $fileId"
            setBackgroundResource(com.jj.ui.R.drawable.j_bg_input)
            load(url)
            setOnClickListener { showPhotoPreview(url) }
        }
    }

    private fun showPhotoPreview(url: String) {
        val popupRoot = FrameLayout(this).apply {
            setBackgroundColor(0xF0000000.toInt())
            setPadding(dp(12), dp(32), dp(12), dp(24))
        }
        val imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            )
            adjustViewBounds = false
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "维修照片大图"
            setOnClickListener { }
        }
        val closeView = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END)
            gravity = Gravity.CENTER
            text = "×"
            textSize = 30f
            setTextColor(Color.WHITE)
            contentDescription = "关闭图片预览"
        }
        popupRoot.addView(imageView)
        popupRoot.addView(closeView)
        val popupWindow = PopupWindow(
            popupRoot,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true,
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        popupRoot.setOnClickListener { popupWindow.dismiss() }
        closeView.setOnClickListener { popupWindow.dismiss() }
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
        popupRoot.post { imageView.load(url) }
    }

    private fun currentToken(): String {
        return JiangStorage.kv.getString(ZdsStorageKeys.MES_UP_TOKEN, "").orEmpty()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val EXTRA_INFO_ID = "extra_info_id"
        private const val EXTRA_SN = "extra_sn"
        private const val EXTRA_SOURCE = "extra_source"
        private const val EXTRA_PLAN = "extra_plan"
        private const val EXTRA_BATCH = "extra_batch"
        private const val EXTRA_NG_PHENOMENA = "extra_ng_phenomena"
        private const val EXTRA_NG_REMARK = "extra_ng_remark"
        private const val EXTRA_USER = "extra_user"
        private const val EXTRA_CREATE_TIME = "extra_create_time"
        private const val EXTRA_STATUS = "extra_status"
        private const val EXTRA_RESULT = "extra_result"
        private const val EXTRA_NG_REASON = "extra_ng_reason"
        private const val EXTRA_REPAIR_BY = "extra_repair_by"
        private const val EXTRA_REPAIR_DESC = "extra_repair_desc"
        private const val EXTRA_FILES = "extra_files"

        fun start(context: Context, report: RepairSnScanValidDto) {
            context.startActivity(
                Intent(context, RepairHistoryActivity::class.java)
                    .putExtra(EXTRA_INFO_ID, report.id?.toLongOrNull() ?: 0L)
                    .putExtra(EXTRA_SN, report.sn)
                    .putExtra(EXTRA_SOURCE, report.ngSource)
                    .putExtra(EXTRA_PLAN, report.planNumber)
                    .putExtra(EXTRA_BATCH, report.batchNumber)
                    .putExtra(EXTRA_NG_PHENOMENA, report.ngPhenomena)
                    .putExtra(EXTRA_NG_REMARK, report.ngRemark)
                    .putExtra(EXTRA_USER, report.userName)
                    .putExtra(EXTRA_CREATE_TIME, report.createTime)
                    .putExtra(EXTRA_STATUS, report.statusName)
                    .putExtra(EXTRA_RESULT, report.result)
                    .putExtra(EXTRA_NG_REASON, report.ngReason)
                    .putExtra(EXTRA_REPAIR_BY, report.repairBy)
                    .putExtra(EXTRA_REPAIR_DESC, report.repairDesc)
                    .putStringArrayListExtra(EXTRA_FILES, ArrayList(report.fileList)),
            )
        }
    }
}
