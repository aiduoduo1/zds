package com.jj.zds.feature.qrcode.ui

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.state.JiangErrorMode
import com.jj.zds.data.dto.qrcode.QrCodeDto
import com.jj.zds.databinding.ActivityQrCodeDecodeBinding
import com.jj.zds.viewmodel.ZdsViewModel

class QrCodeDecodeActivity : JiangToolbarStateVmActivity<ActivityQrCodeDecodeBinding, ZdsViewModel>() {

    private var lastSubmitCode: String = ""
    private var lastSubmitAt: Long = 0L

    override fun createViewBinding(inflater: LayoutInflater): ActivityQrCodeDecodeBinding {
        return ActivityQrCodeDecodeBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.inputQrCode.onTextChanged {
            if (it != lastSubmitCode) {
                lastSubmitCode = ""
            }
        }
        binding.inputQrCode.onTextDebounce(delayMillis = 260L) {
            submitDecode(it)
        }
        binding.inputQrCode.onInputDone {
            submitDecode(it)
        }
        focusScanInput(selectAll = false)
    }

    override fun initPageObserver() {
        viewModel.qrCodeDecodeResult.observe(this) { result ->
            binding.tvResult.text = result.toDisplayText()
            focusScanInput(selectAll = true)
        }
    }

    override fun toolbarTitle(): CharSequence = "二维码解析"

    override fun toolbarSubtitle(): CharSequence = "QR Code Decode"

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        super.onUiError(code, message, throwable)
        focusScanInput(selectAll = true)
        JConfirmDialog(this, this)
            .setTitle("解析失败")
            .setMessage(message)
            .setConfirmText("知道了")
            .setShowCancelButton(false)
            .setOnConfirm {
                focusScanInput(selectAll = true)
            }
            .setOnDismiss {
                focusScanInput(selectAll = true)
            }
            .show()
    }

    private fun submitDecode(rawCode: String) {
        val code = rawCode.trim()
        val now = SystemClock.elapsedRealtime()
        if (code.isBlank()) return
        if (code == lastSubmitCode && now - lastSubmitAt < SUBMIT_DEBOUNCE_WINDOW) return
        lastSubmitCode = code
        lastSubmitAt = now
        viewModel.decodeQrCode(code)
    }

    private fun focusScanInput(selectAll: Boolean) {
        binding.inputQrCode.post {
            binding.inputQrCode.requestInputFocus(showKeyboard = false)
            if (selectAll) {
                binding.inputQrCode.selectAll()
            }
        }
    }

    private fun QrCodeDto.toDisplayText(): String {
        return buildString {
            appendLine("解析成功")
            appendLine()
            appendItem("ID", id)
            appendItem("类型", type)
            appendItem("分类", categoryName)
            appendLine("数量：$num")
            appendItem("批次", lot)
            appendItem("物料编码", materialCode)
            appendItem("软件编码", softCode)
            appendItem("站点编码", siteCode)
            appendItem("流转码", circulationCode)
            appendItem("设备编码", equipCode)
            appendItem("设备分类", equipCategoryCode)
            appendItem("设备类型", equipType)
            appendItem("创建时间", createDate)
        }.trimEnd()
    }

    private fun StringBuilder.appendItem(label: String, value: String?) {
        if (!value.isNullOrBlank()) {
            appendLine("$label：$value")
        }
    }

    private companion object {
        const val SUBMIT_DEBOUNCE_WINDOW = 800L
    }
}
