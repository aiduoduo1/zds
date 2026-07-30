package com.jj.zds.feature.circulation.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.dialog.JSingleChoiceDialog
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.ui.toolbar.JiangToolbarMenuItem
import com.jj.zds.data.dto.circulation.CirculationProductCheckDto
import com.jj.zds.data.dto.circulation.CirculationReasonDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationInfoDto
import com.jj.zds.databinding.ActivityCirculationDiscardBinding

class CirculationAdjustActivity :
    JiangToolbarStateVmActivity<ActivityCirculationDiscardBinding, CirculationAdjustViewModel>() {

    private val mode by lazy {
        CirculationAdjustMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: CirculationAdjustMode.COMBINE.name)
    }
    private var primaryInfo: ProductionCirculationInfoDto? = null
    private var secondaryInfo: ProductionCirculationInfoDto? = null
    private var productInfo: CirculationProductCheckDto? = null
    private var reasons = emptyList<CirculationReasonDto>()
    private var selectedReason: CirculationReasonDto? = null
    private var step = 1
    private var busy = false

    override fun createViewBinding(inflater: LayoutInflater) = ActivityCirculationDiscardBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.inputCirculation.setScanOnly(true)
        (binding.inputCirculation.focusedInputView() as? EditText)?.isCursorVisible = true
        binding.inputCirculation.onTextDebounce(260L, ::submitScan)
        binding.inputCirculation.onInputDone(::submitScan)
        binding.tvReason.setOnClickListener { showReasonDialog() }
        binding.btnTicketDetail.setOnClickListener {
            primaryInfo?.id?.takeIf(String::isNotBlank)?.let { CirculationTicketDetailActivity.start(this, it) }
        }
        binding.btnConfirmDiscard.text = "确认操作"
        binding.btnConfirmDiscard.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, com.jj.ui.R.color.j_color_primary),
        )
        binding.btnConfirmDiscard.setOnClickListener { confirmSubmit() }
        resetPage()
    }

    override fun initPageObserver() {
        viewModel.primaryChecked.observe(this, viewModel::loadPrimaryInfo)
        viewModel.primaryInfo.observe(this) { info ->
            busy = false
            primaryInfo = info
            renderInfo(info)
            startSecondStep()
        }
        viewModel.secondarySheetChecked.observe(this, viewModel::loadSecondaryInfo)
        viewModel.secondaryInfo.observe(this) {
            busy = false
            secondaryInfo = it
            completeSecondStep()
        }
        viewModel.productChecked.observe(this) {
            busy = false
            productInfo = it
            completeSecondStep()
        }
        viewModel.reasons.observe(this) { reasons = it }
        viewModel.operationDone.observe(this) {
            busy = false
            JConfirmDialog(this, this)
                .setTitle("操作成功")
                .setMessage(successMessage())
                .setConfirmText("返回流转单管理")
                .setShowCancelButton(false)
                .setOnConfirm(::finish)
                .show()
        }
    }

    override fun initData() {
        viewModel.loadReasons(mode)
        focusScanner()
    }

    override fun toolbarTitle(): CharSequence = when (mode) {
        CirculationAdjustMode.COMBINE -> "流转单合单"
        CirculationAdjustMode.ADD -> "成品加入"
        CirculationAdjustMode.REMOVE -> "单个成品移除"
    }

    override fun toolbarSubtitle(): CharSequence = when (mode) {
        CirculationAdjustMode.COMBINE -> "主单吸收另一张有效流转单"
        CirculationAdjustMode.ADD -> "无有效流转单成品加入当前单"
        CirculationAdjustMode.REMOVE -> "从当前流转单移除一个成品"
    }

    override fun toolbarMenuItems() = listOf(JiangToolbarMenuItem(MENU_RESET, "重置"))

    override fun onToolbarMenuClick(item: JiangToolbarMenuItem) {
        if (item.id == MENU_RESET) resetPage() else super.onToolbarMenuClick(item)
    }

    override fun showBackButton(): Boolean = true
    override fun enableStateLayout(): Boolean = false
    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        super.onUiError(code, message, throwable)
        busy = false
        if (step == 2 && !secondComplete()) {
            binding.cardTarget.visibility = View.GONE
            updateConfirmButton()
        }
        binding.inputCirculation.showError(message).selectAll()
        focusScanner()
        JToast.show(message)
    }

    private fun submitScan(raw: String) {
        val code = raw.trim()
        if (code.isBlank() || busy || secondComplete()) return
        busy = true
        binding.inputCirculation.verifying("校验中...")
        if (step == 1) {
            viewModel.checkPrimary(mode, code)
        } else {
            val primaryId = primaryInfo?.id?.takeIf(String::isNotBlank)
            if (primaryId == null) {
                busy = false
                binding.inputCirculation.showError("接口未返回主流转单ID")
                return
            }
            clearSecondTarget()
            viewModel.checkSecond(mode, primaryId, code)
        }
    }

    private fun startSecondStep() {
        step = 2
        binding.inputCirculation.clearText().clearState()
        binding.inputCirculation.setTitle(
            when (mode) {
                CirculationAdjustMode.COMBINE -> "扫描被合并流转单"
                CirculationAdjustMode.ADD -> "扫描无有效流转单成品码"
                CirculationAdjustMode.REMOVE -> "扫描当前单内成品码"
            },
        )
        binding.inputCirculation.setHint(if (mode == CirculationAdjustMode.COMBINE) "请扫描另一张流转单" else "请扫描成品码")
        binding.cardReason.visibility = View.VISIBLE
        binding.cardTarget.visibility = View.GONE
        binding.btnConfirmDiscard.visibility = View.VISIBLE
        updateConfirmButton()
        focusScanner()
    }

    private fun completeSecondStep() {
        binding.inputCirculation.showSuccess("校验通过")
        renderTargetComplete()
        updateConfirmButton()
    }

    private fun renderInfo(info: ProductionCirculationInfoDto) {
        binding.cardSheet.visibility = View.VISIBLE
        binding.tvSheetCode.text = info.circulationCode.orDash()
        binding.tvSheetProduct.text = "${info.partName.orDash()} · ${info.specification.orDash()}"
        binding.tvSystemQuantity.text = "${info.num} 台"
        binding.tvProductCode.text = info.partCode.orDash()
        binding.tvProductionTime.text = info.generateTime.orDash()
        binding.tvOperator.text = info.createByName.orDash()
        binding.tvSheetRemark.text = "备注：${info.remark.orDash()}"
    }

    private fun renderTargetWaiting() {
        binding.cardTarget.visibility = View.VISIBLE
        binding.tvTargetSubtitle.text = "等待第二步扫描"
        binding.tvTargetStatus.visibility = View.GONE
        binding.tvTargetName.text = when (mode) {
            CirculationAdjustMode.COMBINE -> "被合并单"
            CirculationAdjustMode.ADD -> "加入成品"
            CirculationAdjustMode.REMOVE -> "移除成品"
        }
        binding.tvTargetDesc.text = when (mode) {
            CirculationAdjustMode.COMBINE -> "请扫描另一张流转单"
            CirculationAdjustMode.ADD, CirculationAdjustMode.REMOVE -> "请扫描成品码"
        }
        binding.tvTargetCode.text = "-"
        binding.tvTargetQuantity.text = ""
    }

    private fun clearSecondTarget() {
        secondaryInfo = null
        productInfo = null
        binding.cardTarget.visibility = View.GONE
        updateConfirmButton()
    }

    private fun renderTargetComplete() {
        binding.cardTarget.visibility = View.VISIBLE
        binding.tvTargetSubtitle.text = "第二步已校验"
        binding.tvTargetStatus.visibility = View.VISIBLE
        when (mode) {
            CirculationAdjustMode.COMBINE -> {
                val mainNum = primaryInfo?.num ?: 0
                val fromNum = secondaryInfo?.num ?: 0
                binding.tvTargetName.text = "被合并单"
                binding.tvTargetDesc.text = "系统数量变化"
                binding.tvTargetCode.text = secondaryInfo?.circulationCode.orDash()
                binding.tvTargetQuantity.text = "$mainNum → ${mainNum + fromNum} 台"
            }
            CirculationAdjustMode.ADD -> {
                val mainNum = primaryInfo?.num ?: 0
                val qty = productInfo?.proMatQty ?: 1
                binding.tvTargetName.text = "加入成品"
                binding.tvTargetDesc.text = "当前数量变化"
                binding.tvTargetCode.text = productInfo?.proMatCode.orDash()
                binding.tvTargetQuantity.text = "$mainNum → ${mainNum + qty} 台"
            }
            CirculationAdjustMode.REMOVE -> {
                val mainNum = primaryInfo?.num ?: 0
                val qty = productInfo?.proMatQty ?: 1
                binding.tvTargetName.text = "移除成品"
                binding.tvTargetDesc.text = "当前数量变化"
                binding.tvTargetCode.text = productInfo?.proMatCode.orDash()
                binding.tvTargetQuantity.text = "$mainNum → ${(mainNum - qty).coerceAtLeast(0)} 台"
            }
        }
    }

    private fun showReasonDialog() {
        if (reasons.isEmpty()) return JToast.show("操作原因尚未加载")
        JSingleChoiceDialog<CirculationReasonDto>(this, this)
            .setTitle("选择操作原因")
            .setItems(reasons) { it.label.orEmpty() }
            .setSelectedIndex(reasons.indexOf(selectedReason))
            .setOnConfirm {
                selectedReason = it
                binding.tvReason.text = it?.label ?: "请选择操作原因"
                updateConfirmButton()
            }
            .show()
    }

    private fun confirmSubmit() {
        val primaryId = primaryInfo?.id?.takeIf(String::isNotBlank) ?: return JToast.show("请先扫描流转单")
        val targetId = when (mode) {
            CirculationAdjustMode.COMBINE -> secondaryInfo?.id
            CirculationAdjustMode.ADD, CirculationAdjustMode.REMOVE -> productInfo?.proMatId ?: productInfo?.id
        }?.takeIf(String::isNotBlank) ?: return JToast.show("请完成第二步扫码")
        val reason = selectedReason?.valueStr?.toIntOrNull() ?: return JToast.show("请选择操作原因")
        JConfirmDialog(this, this)
            .setTitle("确认执行${toolbarTitle()}？")
            .setMessage("流转单：${primaryInfo?.circulationCode.orDash()}\n操作提交后将立即生效。")
            .setConfirmText("确认执行")
            .setOnConfirm {
                busy = true
                updateConfirmButton()
                viewModel.submit(mode, primaryId, targetId, reason, binding.editRemark.text?.toString().orEmpty().trim())
            }
            .show()
    }

    private fun resetPage() {
        busy = false
        step = 1
        primaryInfo = null
        secondaryInfo = null
        productInfo = null
        selectedReason = null
        binding.inputCirculation.clearText().clearState()
            .setTitle("请扫描流转单码")
            .setHint("请扫描流转单号")
        binding.cardSheet.visibility = View.GONE
        binding.cardTarget.visibility = View.GONE
        binding.cardReason.visibility = View.GONE
        binding.btnConfirmDiscard.visibility = View.GONE
        binding.tvReason.text = "请选择操作原因"
        binding.editRemark.setText("")
        focusScanner()
    }

    private fun updateConfirmButton() {
        binding.btnConfirmDiscard.isEnabled = secondComplete() && selectedReason != null && !busy
        binding.btnConfirmDiscard.alpha = if (binding.btnConfirmDiscard.isEnabled) 1f else 0.6f
    }

    private fun secondComplete() = secondaryInfo != null || productInfo != null
    private fun focusScanner() = binding.inputCirculation.post { binding.inputCirculation.requestInputFocus(false) }
    private fun successMessage() = when (mode) {
        CirculationAdjustMode.COMBINE -> "来源流转单已合入主单。"
        CirculationAdjustMode.ADD -> "成品已加入当前流转单。"
        CirculationAdjustMode.REMOVE -> "成品已从当前流转单移除。"
    }
    private fun String?.orDash() = this?.takeIf(String::isNotBlank) ?: "-"

    companion object {
        private const val EXTRA_MODE = "extra_mode"
        private const val MENU_RESET = 1
        fun start(context: Context, mode: CirculationAdjustMode) {
            context.startActivity(Intent(context, CirculationAdjustActivity::class.java).putExtra(EXTRA_MODE, mode.name))
        }
    }
}
