package com.jj.zds.feature.repair.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import com.jj.ui.base.JiangToolbarActivity
import com.jj.ui.widget.JStatusType
import com.jj.zds.data.dto.repair.RepairSnScanValidDto
import com.jj.zds.databinding.ActivityRepairDetailBinding

class RepairDetailActivity : JiangToolbarActivity<ActivityRepairDetailBinding>() {

    private var remarkExpanded = false
    private var remarkCanExpand = false

    override fun createViewBinding(inflater: LayoutInflater): ActivityRepairDetailBinding {
        return ActivityRepairDetailBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.tvSn.text = "SN  ${extra(EXTRA_SN)}"
        binding.tvSource.text = "来源  ${extra(EXTRA_SOURCE)}"
        binding.tvPlan.text = "单号  ${extra(EXTRA_PLAN)}"
        binding.tvBatch.text = "批次  ${extra(EXTRA_BATCH)}"
        binding.tvMaterial.text = "物料  ${extra(EXTRA_MATERIAL)}"
        binding.tvTestMaterial.text = "测试物料  ${extra(EXTRA_TEST_MATERIAL)}"
        binding.tvNgPhenomena.text = extra(EXTRA_NG_PHENOMENA)
        binding.tvReporter.text = "上报人  ${extra(EXTRA_USER)}"
        binding.tvReportTime.text = "上报时间  ${extra(EXTRA_CREATE_TIME)}"
        val status = extra(EXTRA_STATUS)
        binding.tagStatus.setStatus(status, status.toStatusType())
        renderRemark(RepairRemarkDisplay.format(intent.getStringExtra(EXTRA_NG_REMARK)).orDash())
        binding.btnToggleRemark.setOnClickListener { toggleRemark() }
        binding.root.post { binding.root.scrollTo(0, 0) }
    }

    override fun toolbarTitle(): CharSequence = "详情信息"

    override fun toolbarSubtitle(): CharSequence = extra(EXTRA_SN)

    private fun renderRemark(remark: String) {
        binding.tvNgRemark.maxLines = Int.MAX_VALUE
        binding.tvNgRemark.ellipsize = null
        binding.tvNgRemark.text = remark
        binding.tvNgRemark.post {
            remarkCanExpand = binding.tvNgRemark.lineCount > COLLAPSED_LINES
            if (remarkCanExpand) {
                binding.tvNgRemark.maxLines = COLLAPSED_LINES
                binding.tvNgRemark.ellipsize = TextUtils.TruncateAt.END
                binding.btnToggleRemark.visibility = View.VISIBLE
            }
        }
    }

    private fun toggleRemark() {
        if (!remarkCanExpand) return
        remarkExpanded = !remarkExpanded
        binding.tvNgRemark.maxLines = if (remarkExpanded) Int.MAX_VALUE else COLLAPSED_LINES
        binding.tvNgRemark.ellipsize = if (remarkExpanded) null else TextUtils.TruncateAt.END
        binding.btnToggleRemark.text = if (remarkExpanded) "收起" else "展开"
    }

    private fun extra(key: String): String = intent.getStringExtra(key).orDash()

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
        private const val EXTRA_SN = "extra_sn"
        private const val EXTRA_SOURCE = "extra_source"
        private const val EXTRA_PLAN = "extra_plan"
        private const val EXTRA_BATCH = "extra_batch"
        private const val EXTRA_MATERIAL = "extra_material"
        private const val EXTRA_TEST_MATERIAL = "extra_test_material"
        private const val EXTRA_NG_PHENOMENA = "extra_ng_phenomena"
        private const val EXTRA_NG_REMARK = "extra_ng_remark"
        private const val EXTRA_USER = "extra_user"
        private const val EXTRA_CREATE_TIME = "extra_create_time"
        private const val EXTRA_STATUS = "extra_status"
        private const val COLLAPSED_LINES = 3

        fun start(context: Context, report: RepairSnScanValidDto) {
            context.startActivity(
                Intent(context, RepairDetailActivity::class.java)
                    .putExtra(EXTRA_SN, report.sn)
                    .putExtra(EXTRA_SOURCE, report.ngSource)
                    .putExtra(EXTRA_PLAN, report.planNumber)
                    .putExtra(EXTRA_BATCH, report.batchNumber)
                    .putExtra(EXTRA_MATERIAL, report.categoryName)
                    .putExtra(EXTRA_TEST_MATERIAL, report.productName)
                    .putExtra(EXTRA_NG_PHENOMENA, report.ngPhenomena)
                    .putExtra(EXTRA_NG_REMARK, report.ngRemark)
                    .putExtra(EXTRA_USER, report.userName ?: report.createByName ?: report.createBy)
                    .putExtra(EXTRA_CREATE_TIME, report.createTime)
                    .putExtra(EXTRA_STATUS, report.statusName),
            )
        }
    }
}
