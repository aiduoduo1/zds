package com.jj.zds.feature.circulation.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.zds.data.dto.circulation.CirculationCombineRequest
import com.jj.zds.data.dto.circulation.CirculationProductCheckDto
import com.jj.zds.data.dto.circulation.CirculationProductOperationRequest
import com.jj.zds.data.dto.circulation.CirculationReasonDto
import com.jj.zds.data.dto.productionplan.ProductionCirculationInfoDto
import com.jj.zds.data.repository.ZdsRepository

class CirculationAdjustViewModel : JiangViewModel() {
    private val repository by lazy { ZdsRepository() }

    private val _primaryChecked = MutableLiveData<String>()
    val primaryChecked: LiveData<String> = _primaryChecked
    private val _primaryInfo = MutableLiveData<ProductionCirculationInfoDto>()
    val primaryInfo: LiveData<ProductionCirculationInfoDto> = _primaryInfo
    private val _secondarySheetChecked = MutableLiveData<String>()
    val secondarySheetChecked: LiveData<String> = _secondarySheetChecked
    private val _secondaryInfo = MutableLiveData<ProductionCirculationInfoDto>()
    val secondaryInfo: LiveData<ProductionCirculationInfoDto> = _secondaryInfo
    private val _productChecked = MutableLiveData<CirculationProductCheckDto>()
    val productChecked: LiveData<CirculationProductCheckDto> = _productChecked
    private val _reasons = MutableLiveData<List<CirculationReasonDto>>()
    val reasons: LiveData<List<CirculationReasonDto>> = _reasons
    private val _operationDone = MutableLiveData<Unit>()
    val operationDone: LiveData<Unit> = _operationDone

    fun checkPrimary(mode: CirculationAdjustMode, qrCode: String) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                when (mode) {
                    CirculationAdjustMode.COMBINE -> repository.checkCirculationCombineMain(qrCode)
                    CirculationAdjustMode.ADD -> repository.checkAddProductCirculation(qrCode)
                    CirculationAdjustMode.REMOVE -> repository.checkRemoveProductCirculation(qrCode)
                }
            },
            onSuccess = { _primaryChecked.value = qrCode },
        )
    }

    fun loadPrimaryInfo(qrCode: String) = loadInfo(qrCode) { _primaryInfo.value = it }
    fun loadSecondaryInfo(qrCode: String) = loadInfo(qrCode) { _secondaryInfo.value = it }

    private fun loadInfo(qrCode: String, success: (ProductionCirculationInfoDto) -> Unit) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getProductionCirculationInfoByQrCode(qrCode) },
            onSuccess = success,
        )
    }

    fun checkSecond(mode: CirculationAdjustMode, primaryId: String, code: String) {
        when (mode) {
            CirculationAdjustMode.COMBINE -> launchResult(
                loadingMode = JiangLoadingMode.NONE,
                block = { repository.checkCirculationCombineFrom(code, primaryId) },
                onSuccess = { _secondarySheetChecked.value = code },
            )
            CirculationAdjustMode.ADD -> launchResult(
                loadingMode = JiangLoadingMode.NONE,
                block = { repository.checkCirculationAddProduct(primaryId, code) },
                onSuccess = { _productChecked.value = it.firstOrNull() ?: return@launchResult },
            )
            CirculationAdjustMode.REMOVE -> launchResult(
                loadingMode = JiangLoadingMode.NONE,
                block = { repository.checkCirculationRemoveProduct(primaryId, code) },
                onSuccess = { _productChecked.value = it.firstOrNull() ?: return@launchResult },
            )
        }
    }

    fun loadReasons(mode: CirculationAdjustMode) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                when (mode) {
                    CirculationAdjustMode.COMBINE -> repository.getCirculationCombineReasons()
                    CirculationAdjustMode.ADD -> repository.getCirculationAddReasons()
                    CirculationAdjustMode.REMOVE -> repository.getCirculationRemoveReasons()
                }
            },
            onSuccess = { _reasons.value = it },
        )
    }

    fun submit(
        mode: CirculationAdjustMode,
        primaryId: String,
        secondaryId: String,
        reason: Int,
        remark: String,
    ) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            loadingMessage = "提交中...",
            block = {
                when (mode) {
                    CirculationAdjustMode.COMBINE -> repository.combineCirculation(
                        CirculationCombineRequest(primaryId, secondaryId, reason, remark),
                    )
                    CirculationAdjustMode.ADD -> repository.addCirculationProduct(
                        CirculationProductOperationRequest(primaryId, secondaryId, reason, remark),
                    )
                    CirculationAdjustMode.REMOVE -> repository.removeCirculationProduct(
                        CirculationProductOperationRequest(primaryId, secondaryId, reason, remark),
                    )
                }
            },
            onSuccess = { _operationDone.value = Unit },
        )
    }
}

enum class CirculationAdjustMode { COMBINE, ADD, REMOVE }
