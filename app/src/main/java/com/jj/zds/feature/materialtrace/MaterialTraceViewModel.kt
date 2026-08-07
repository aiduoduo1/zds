package com.jj.zds.feature.materialtrace

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.zds.data.dto.materialtrace.MaterialTraceDetailDto
import com.jj.zds.data.repository.ZdsRepository

class MaterialTraceViewModel : JiangViewModel() {

    private val repository by lazy { ZdsRepository() }

    private val _traceDetail = MutableLiveData<MaterialTraceDetailDto>()
    val traceDetail: LiveData<MaterialTraceDetailDto> = _traceDetail

    fun query(qrCode: String) {
        launchResult(
            loadingMode = JiangLoadingMode.NONE,
            block = { repository.getMaterialTrace(qrCode.trim()) },
            onSuccess = { _traceDetail.value = it },
        )
    }
}
