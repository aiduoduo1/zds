package com.jj.zds.feature.circulation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.dialog.JSingleChoiceDialog
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.data.dto.circulation.CirculationReasonDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationInfoDto
import com.jj.zds.databinding.ActivityCirculationDiscardBinding

class CirculationDiscardActivity :
    JiangToolbarStateVmActivity<ActivityCirculationDiscardBinding, CirculationDiscardViewModel>() {

    private var scanInProgress = false
    private var submitInProgress = false
    private var currentInfo: ProductionCirculationInfoDto? = null
    private var reasons = emptyList<CirculationReasonDto>()
    private var selectedReason: CirculationReasonDto? = null
    private var activeOperation = Operation.NONE

    override fun createViewBinding(inflater: LayoutInflater): ActivityCirculationDiscardBinding {
        return ActivityCirculationDiscardBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.inputCirculation.setScanOnly(true)
        (binding.inputCirculation.focusedInputView() as? EditText)?.isCursorVisible = true
        binding.inputCirculation.onTextDebounce(delayMillis = 260L, listener = ::submitScan)
        binding.inputCirculation.onInputDone(::submitScan)
        binding.inputCirculation.onTextChanged { text ->
            if (text.isBlank() && currentInfo != null && !scanInProgress && !submitInProgress) {
                clearLoadedSheet()
            }
        }
        binding.tvReason.setOnClickListener { showReasonDialog() }
        binding.btnTicketDetail.setOnClickListener {
            currentInfo?.id?.takeIf(String::isNotBlank)?.let {
                CirculationTicketDetailActivity.start(this, it)
            }
        }
        binding.btnConfirmDiscard.setOnClickListener { confirmDiscard() }
        renderInfo(null)
    }

    override fun initPageObserver() {
        viewModel.checkDone.observe(this) { qrCode ->
            activeOperation = Operation.LOAD_INFO
            viewModel.loadInfo(qrCode)
        }
        viewModel.circulationInfo.observe(this) { info ->
            scanInProgress = false
            activeOperation = Operation.NONE
            currentInfo = info
            binding.inputCirculation.showSuccess("流转单校验通过")
            renderInfo(info)
        }
        viewModel.reasons.observe(this) {
            reasons = it
            if (activeOperation == Operation.LOAD_REASONS) activeOperation = Operation.NONE
        }
        viewModel.discardDone.observe(this) {
            submitInProgress = false
            activeOperation = Operation.NONE
            JConfirmDialog(this, this)
                .setTitle("作废成功")
                .setMessage("流转单已永久作废，关联成品已解除有效归属。")
                .setConfirmText("返回流转单管理")
                .setShowCancelButton(false)
                .setOnConfirm(::finish)
                .show()
        }
    }

    override fun initData() {
        activeOperation = Operation.LOAD_REASONS
        viewModel.loadReasons()
        binding.inputCirculation.post { binding.inputCirculation.requestInputFocus(showKeyboard = false) }
    }

    override fun toolbarTitle(): CharSequence = "流转单作废"

    override fun toolbarSubtitle(): CharSequence = "扫描流转单并确认永久作废"

    override fun showBackButton(): Boolean = true

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        super.onUiError(code, message, throwable)
        when (activeOperation) {
            Operation.CHECK, Operation.LOAD_INFO -> {
                scanInProgress = false
                clearLoadedSheet()
                binding.inputCirculation.showError(message).selectAll()
            }
            Operation.DISCARD -> submitInProgress = false
            Operation.LOAD_REASONS, Operation.NONE -> Unit
        }
        activeOperation = Operation.NONE
        if (message.isNotBlank()) JToast.show(message)
    }

    private fun submitScan(rawCode: String) {
        val qrCode = rawCode.trim()
        if (qrCode.isBlank() || scanInProgress || submitInProgress) return
        scanInProgress = true
        activeOperation = Operation.CHECK
        clearLoadedSheet(clearInput = false)
        binding.inputCirculation.verifying("流转单校验中...")
        viewModel.check(qrCode)
    }

    private fun showReasonDialog() {
        if (reasons.isEmpty()) {
            JToast.show("作废原因尚未加载")
            return
        }
        val selectedIndex = reasons.indexOf(selectedReason)
        JSingleChoiceDialog<CirculationReasonDto>(this, this)
            .setTitle("选择作废原因")
            .setItems(reasons) { it.label.orEmpty() }
            .setSelectedIndex(selectedIndex)
            .setOnConfirm { reason ->
                selectedReason = reason
                binding.tvReason.text = reason?.label ?: "请选择作废原因"
                updateConfirmButton()
            }
            .show()
    }

    private fun confirmDiscard() {
        val info = currentInfo ?: return JToast.show("请先扫描流转单")
        val reason = selectedReason ?: return JToast.show("请选择作废原因")
        val reasonValue = reason.valueStr?.toIntOrNull()
            ?: return JToast.show("作废原因参数无效")
        val id = info.id?.takeIf(String::isNotBlank)
            ?: return JToast.show("接口未返回流转单ID")
        JConfirmDialog(this, this)
            .setTitle("确认作废该流转单？")
            .setMessage("${info.circulationCode.orDash()}\n作废后永久失效且不可恢复，关联成品将解除有效归属。")
            .setConfirmText("确认作废")
            .setOnConfirm {
                submitInProgress = true
                activeOperation = Operation.DISCARD
                updateConfirmButton()
                viewModel.discard(id, reasonValue, binding.editRemark.text?.toString().orEmpty().trim())
            }
            .show()
    }

    private fun renderInfo(info: ProductionCirculationInfoDto?) {
        val visible = info != null
        binding.cardSheet.visibility = if (visible) View.VISIBLE else View.GONE
        binding.cardReason.visibility = if (visible) View.VISIBLE else View.GONE
        binding.btnConfirmDiscard.visibility = if (visible) View.VISIBLE else View.GONE
        if (info == null) return
        binding.tvSheetCode.text = info.circulationCode.orDash()
        binding.tvSheetProduct.text = "${info.partName.orDash()} · ${info.specification.orDash()}"
        binding.tvSheetStatus.text = if (info.valid == true) "有效" else "失效"
        binding.tvFaceQuantity.text = "${info.faceNum} 台"
        binding.tvSystemQuantity.text = "${info.num} 台"
        binding.tvProductName.text = info.partName.orDash()
        binding.tvProductCode.text = info.partCode.orDash()
        binding.tvProductionTime.text = info.generateTime.orDash()
        binding.tvOperator.text = info.createByName.orDash()
        binding.tvSheetRemark.text = "备注：${info.remark.orDash()}"
        updateConfirmButton()
    }

    private fun clearLoadedSheet(clearInput: Boolean = false) {
        currentInfo = null
        selectedReason = null
        binding.tvReason.text = "请选择作废原因"
        binding.editRemark.setText("")
        if (clearInput) binding.inputCirculation.clearText().clearState()
        renderInfo(null)
    }

    private fun updateConfirmButton() {
        binding.btnConfirmDiscard.isEnabled = currentInfo != null && selectedReason != null && !submitInProgress
        binding.btnConfirmDiscard.alpha = if (binding.btnConfirmDiscard.isEnabled) 1f else 0.6f
    }

    private fun String?.orDash(): String = this?.takeIf(String::isNotBlank) ?: "-"

    private enum class Operation { NONE, LOAD_REASONS, CHECK, LOAD_INFO, DISCARD }
}
