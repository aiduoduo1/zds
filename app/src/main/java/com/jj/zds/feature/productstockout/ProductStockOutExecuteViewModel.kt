package com.jj.zds.feature.productstockout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.zds.data.dto.productstockout.ProductStockOutDetailDto
import com.jj.zds.data.repository.ZdsRepository

class ProductStockOutExecuteViewModel : JiangViewModel() {

    private val repository by lazy { ZdsRepository() }

    private val _detail = MutableLiveData<ProductStockOutDetailDto>()
    val detail: LiveData<ProductStockOutDetailDto> = _detail

    private val _scanSubmitted = MutableLiveData<Unit>()
    val scanSubmitted: LiveData<Unit> = _scanSubmitted

    private val _recordDeleted = MutableLiveData<Unit>()
    val recordDeleted: LiveData<Unit> = _recordDeleted

    private val _childTaskFinished = MutableLiveData<Unit>()
    val childTaskFinished: LiveData<Unit> = _childTaskFinished

    fun loadDetail(taskDetailId: String) {
        launchResult(
            loadingMode = JiangLoadingMode.PAGE,
            block = { repository.getProductStockOutDetail(taskDetailId) },
            onSuccess = { _detail.value = it },
        )
    }

    fun submitScan(taskDetailId: String, qrCode: String) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.submitProductStockOut(taskDetailId, qrCode) },
            onSuccess = { _scanSubmitted.value = Unit },
        )
    }

    fun deleteRecord(recordId: String) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            block = { repository.deleteProductStockOutRecord(recordId) },
            onSuccess = { _recordDeleted.value = Unit },
        )
    }

    fun finishChildTask(taskDetailId: String) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            block = { repository.finishProductStockOutChildTask(taskDetailId) },
            onSuccess = { _childTaskFinished.value = Unit },
        )
    }
}
