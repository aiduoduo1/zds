package com.jj.zds.feature.aging

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.zds.data.dto.aging.AgingEndDetailDto
import com.jj.zds.data.dto.aging.AgingProductDto
import com.jj.zds.data.dto.debugplan.DebugDictItemDto
import com.jj.zds.data.repository.ZdsRepository

class AgingViewModel : JiangViewModel() {

    private val repository by lazy { ZdsRepository() }

    private val _pendingProducts = MutableLiveData<List<AgingProductDto>>(emptyList())
    val pendingProducts: LiveData<List<AgingProductDto>> = _pendingProducts

    private val _scanMergeResult = MutableLiveData<AgingPendingMergeResult>()
    val scanMergeResult: LiveData<AgingPendingMergeResult> = _scanMergeResult

    private val _startDone = MutableLiveData<Int>()
    val startDone: LiveData<Int> = _startDone

    private val _endDetail = MutableLiveData<AgingEndDetailDto?>()
    val endDetail: LiveData<AgingEndDetailDto?> = _endDetail

    private val _endDone = MutableLiveData<Unit>()
    val endDone: LiveData<Unit> = _endDone

    private val _ngPhenomenaItems = MutableLiveData<List<DebugDictItemDto>>()
    val ngPhenomenaItems: LiveData<List<DebugDictItemDto>> = _ngPhenomenaItems

    fun scan(qrCode: String) {
        val code = qrCode.trim()
        if (code.isBlank()) {
            showToast("请扫描流转单或PRO码")
            return
        }
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.scanValidAgingProduct(code) },
            onSuccess = { incoming ->
                val result = AgingPendingListRules.merge(_pendingProducts.value.orEmpty(), incoming)
                _pendingProducts.value = result.products
                _scanMergeResult.value = result
            },
        )
    }

    fun remove(product: AgingProductDto) {
        val proCode = product.materialCode?.trim()
        _pendingProducts.value = _pendingProducts.value.orEmpty().filterNot {
            it.materialCode?.trim().equals(proCode, ignoreCase = true)
        }
    }

    fun clearAll() {
        _pendingProducts.value = emptyList()
        _endDetail.value = null
    }

    fun submit() {
        val products = AgingPendingListRules.merge(
            existing = emptyList(),
            incoming = _pendingProducts.value.orEmpty(),
        ).products
        if (products.isEmpty()) {
            showToast("请先扫描待老化产品")
            return
        }
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.startAging(products) },
            onSuccess = {
                _pendingProducts.value = emptyList()
                _startDone.value = products.size
            },
        )
    }

    fun scanEnd(qrCode: String) {
        val code = qrCode.trim()
        if (code.isBlank()) {
            showToast("请扫描PRO码")
            return
        }
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.scanValidAgingEnd(code) },
            onSuccess = { detail ->
                _endDetail.value = detail
            },
        )
    }

    fun loadNgPhenomena() {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getDebugDictItems(DICT_NG_PHENOMENA) },
            onSuccess = { items ->
                _ngPhenomenaItems.value = items
            },
        )
    }

    fun endAging(result: String, ngPhenomena: String = "") {
        val detailId = _endDetail.value?.id.orEmpty()
        if (detailId.isBlank()) {
            showToast("请先扫描待结束老化的PRO码")
            return
        }
        if (result == RESULT_NG && ngPhenomena.isBlank()) {
            showToast("请选择不良现象")
            return
        }
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = {
                repository.endAging(
                    id = detailId,
                    agingResult = result,
                    ngPhenomena = if (result == RESULT_OK) "" else ngPhenomena.trim(),
                )
            },
            onSuccess = {
                _endDetail.value = null
                _endDone.value = Unit
            },
        )
    }

    companion object {
        const val RESULT_OK = "OK"
        const val RESULT_NG = "NG"
        private const val DICT_NG_PHENOMENA = "ngPhenomena"
    }
}
