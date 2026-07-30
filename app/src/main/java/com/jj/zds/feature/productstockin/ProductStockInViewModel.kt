package com.jj.zds.feature.productstockin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.zds.data.dto.productstockin.ProductStockInRecordDto
import com.jj.zds.data.repository.ZdsRepository

class ProductStockInViewModel : JiangViewModel() {

    private val repository by lazy { ZdsRepository() }

    private val _stockInRecords = MutableLiveData<List<ProductStockInRecordDto>>(emptyList())
    val stockInRecords: LiveData<List<ProductStockInRecordDto>> = _stockInRecords

    fun stockIn(qrCode: String) {
        val code = qrCode.trim()
        if (code.isBlank()) {
            showToast("请扫描流转单或成品码")
            return
        }
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.productStockIn(code) },
            onSuccess = { records -> _stockInRecords.value = records },
        )
    }
}
