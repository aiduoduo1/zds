package com.jj.zds.feature.dimmer

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.data.dto.productreturn.AirportDto
import com.jj.zds.databinding.ActivityDimmerBindingBinding
import com.jj.zds.viewmodel.ZdsViewModel

class DimmerBindingActivity : JiangToolbarStateVmActivity<ActivityDimmerBindingBinding, ZdsViewModel>() {
    private var stationQr = ""
    private var proQr = ""
    private var airports = emptyList<AirportDto>()
    private var unbind = false
    private var scanInProgress = false
    private var scanStep = ScanStep.STATION
    private var lastSubmitCode = ""
    private var lastSubmitAt = 0L

    override fun createViewBinding(inflater: LayoutInflater) = ActivityDimmerBindingBinding.inflate(inflater)
    override fun toolbarTitle() = "调光器工位绑定"
    override fun toolbarSubtitle() = "扫描工位与 PRO 标签"
    override fun showBackButton() = true
    override fun errorMode() = JiangErrorMode.TOAST

    override fun initView(savedInstanceState: Bundle?) {
        bindScanInputs()
        binding.btnConfirm.setOnClickListener {
            if (unbind) {
                viewModel.unbindDimmer(stationQr, proQr)
            } else {
                val airport = airports.getOrNull(binding.spinnerAirport.selectedItemPosition)?.name.orEmpty()
                viewModel.bindDimmer(stationQr, proQr, airport, binding.inputProject.text.toString().trim())
            }
        }
        viewModel.loadDimmerAirports()
        focusStationInput()
    }

    override fun initPageObserver() {
        viewModel.dimmerAirports.observe(this) {
            airports = it
            binding.spinnerAirport.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, it.map { airport -> airport.name.orEmpty() })
        }
        viewModel.dimmerBindingContext.observe(this) { context ->
            scanInProgress = false
            unbind = context.action == "UNBIND"
            binding.tvStation.text = "已识别工位：${context.stationCode.orEmpty()}  ${context.stationName.orEmpty()}"
            binding.tvStation.isVisible = true
            binding.inputStation.showSuccess(context.stationName.orEmpty())
            binding.inputPro.isVisible = true
            binding.tvHint.text = if (unbind) "步骤 2 / 2：该工位已有绑定，请扫描原 PRO 标签解绑" else "步骤 2 / 2：扫描 PRO 标签，核对后绑定"

            context.pro?.let { pro ->
                binding.inputPro.showSuccess("已识别 ${pro.proCode.orEmpty()}")
                binding.layoutBindingInfo.isVisible = true
                binding.tvPro.text = "PRO：${pro.proCode.orEmpty()}\n计划：${pro.planNo.orEmpty()}\n产品：${pro.partName ?: pro.partCode.orEmpty()}"
                binding.spinnerAirport.isVisible = !unbind
                binding.tvAirportTitle.isVisible = !unbind
                binding.inputProject.isVisible = !unbind
                binding.btnConfirm.isVisible = true
                binding.btnConfirm.text = if (unbind) "确认解绑" else "确认绑定"
            } ?: focusProInput()
        }
        viewModel.dimmerBindingDone.observe(this) { bound ->
            JToast.show(if (bound) "调光器工位绑定成功" else "调光器工位解绑成功")
            finish()
        }
    }

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        val failedStep = scanStep
        scanInProgress = false
        if (failedStep == ScanStep.STATION) {
            binding.inputStation.showError(message).selectAll()
            focusStationInput(selectAll = true)
        } else {
            binding.inputPro.showError(message).selectAll()
            focusProInput(selectAll = true)
        }
        super.onUiError(code, message, throwable)
    }

    private fun bindScanInputs() {
        binding.inputStation.onTextChanged {
            stationQr = it.trim()
            if (it != lastSubmitCode) lastSubmitCode = ""
            binding.inputStation.clearState()
            clearProState()
        }
        binding.inputStation.onInputDone(::submitStation)
        binding.inputStation.onTextDebounce(delayMillis = 260L, listener = ::submitStation)
        binding.inputPro.onTextChanged {
            proQr = it.trim()
            if (it != lastSubmitCode) lastSubmitCode = ""
            binding.inputPro.clearState()
            binding.layoutBindingInfo.isVisible = false
            binding.btnConfirm.isVisible = false
        }
        binding.inputPro.onInputDone(::submitPro)
        binding.inputPro.onTextDebounce(delayMillis = 260L, listener = ::submitPro)
    }

    private fun submitStation(rawCode: String) {
        val code = rawCode.trim()
        if (!canSubmit(code)) return
        scanStep = ScanStep.STATION
        stationQr = code
        scanInProgress = true
        binding.inputStation.verifying("工位校验中...")
        viewModel.loadDimmerBinding(stationQr)
    }

    private fun submitPro(rawCode: String) {
        val code = rawCode.trim()
        if (stationQr.isBlank()) {
            JToast.show("请先扫描调光器工位")
            focusStationInput()
            return
        }
        if (!canSubmit(code)) return
        scanStep = ScanStep.PRO
        proQr = code
        scanInProgress = true
        binding.inputPro.verifying("PRO 校验中...")
        viewModel.loadDimmerBinding(stationQr, proQr)
    }

    private fun canSubmit(code: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (code.isBlank() || scanInProgress) return false
        if (code == lastSubmitCode && now - lastSubmitAt < SUBMIT_DEBOUNCE_WINDOW) return false
        lastSubmitCode = code
        lastSubmitAt = now
        return true
    }

    private fun clearProState() {
        proQr = ""
        unbind = false
        binding.inputPro.setText("")
        binding.inputPro.clearState()
        binding.inputPro.isVisible = false
        binding.layoutBindingInfo.isVisible = false
        binding.btnConfirm.isVisible = false
        binding.tvHint.text = "步骤 1 / 2：扫描调光器工位"
    }

    private fun focusStationInput(selectAll: Boolean = false) {
        binding.inputStation.post {
            binding.inputStation.requestInputFocus(showKeyboard = false)
            if (selectAll) binding.inputStation.selectAll()
        }
    }

    private fun focusProInput(selectAll: Boolean = false) {
        binding.inputPro.post {
            binding.inputPro.requestInputFocus(showKeyboard = false)
            if (selectAll) binding.inputPro.selectAll()
        }
    }

    private enum class ScanStep { STATION, PRO }

    private companion object {
        const val SUBMIT_DEBOUNCE_WINDOW = 800L
    }
}
