package com.jj.zds.feature.productreturn

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.zds.data.dto.productreturn.ProductReturnScanResultDto
import com.jj.zds.data.repository.ZdsRepository

class ProductReturnExecuteViewModel : JiangViewModel() {
    private val repository by lazy { ZdsRepository() }
    private val _scanResult = MutableLiveData<ProductReturnScanResultDto>()
    val scanResult: LiveData<ProductReturnScanResultDto> = _scanResult
    private val _saved = MutableLiveData<Unit>()
    val saved: LiveData<Unit> = _saved
    private val _finished = MutableLiveData<Unit>()
    val finished: LiveData<Unit> = _finished

    fun scan(taskId: String, qrCode: String) = launchResult(
        loadingMode = JiangLoadingMode.NONE,
        block = { repository.scanProductReturn(taskId, qrCode) },
        onSuccess = { _scanResult.value = it },
    )

    fun save(taskId: String, proIdList: List<String>) = launchResult(
        loadingMode = JiangLoadingMode.DIALOG,
        block = { repository.saveProductReturn(taskId, proIdList) },
        onSuccess = { _saved.value = Unit },
    )

    fun finish(taskId: String) = launchResult(
        loadingMode = JiangLoadingMode.DIALOG,
        block = { repository.finishProductReturnTask(taskId) },
        onSuccess = { _finished.value = Unit },
    )
}
