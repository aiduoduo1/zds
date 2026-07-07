package com.jj.zds.feature.circulation.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.zds.data.dto.circulation.CirculationDiscardRequest
import com.jj.zds.data.dto.circulation.CirculationReasonDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationInfoDto
import com.jj.zds.data.repository.ZdsRepository

class CirculationDiscardViewModel : JiangViewModel() {

    private val repository by lazy { ZdsRepository() }

    private val _checkDone = MutableLiveData<String>()
    val checkDone: LiveData<String> = _checkDone

    private val _circulationInfo = MutableLiveData<ProductionCirculationInfoDto>()
    val circulationInfo: LiveData<ProductionCirculationInfoDto> = _circulationInfo

    private val _reasons = MutableLiveData<List<CirculationReasonDto>>()
    val reasons: LiveData<List<CirculationReasonDto>> = _reasons

    private val _discardDone = MutableLiveData<Unit>()
    val discardDone: LiveData<Unit> = _discardDone

    fun check(qrCode: String) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.checkCirculationDiscard(qrCode) },
            onSuccess = { _checkDone.value = qrCode },
        )
    }

    fun loadInfo(qrCode: String) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getProductionCirculationInfoByQrCode(qrCode) },
            onSuccess = { _circulationInfo.value = it },
        )
    }

    fun loadReasons() {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = repository::getCirculationDiscardReasons,
            onSuccess = { _reasons.value = it },
        )
    }

    fun discard(id: String, operateReason: Int, remark: String) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "作废中...",
            block = {
                repository.discardCirculation(
                    CirculationDiscardRequest(
                        id = id,
                        operateReason = operateReason,
                        remark = remark,
                    ),
                )
            },
            onSuccess = { _discardDone.value = Unit },
        )
    }
}
