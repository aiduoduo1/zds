package com.jj.zds.feature.productstockout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.zds.data.dto.productstockout.ProductStockOutTaskDto
import com.jj.zds.data.repository.ZdsRepository

class ProductStockOutTaskListViewModel : JiangViewModel() {

    private val repository by lazy { ZdsRepository() }

    private val _tasks = MutableLiveData<List<ProductStockOutTaskDto>>(emptyList())
    val tasks: LiveData<List<ProductStockOutTaskDto>> = _tasks

    fun loadTasks() {
        launchResult(
            loadingMode = JiangLoadingMode.PAGE,
            block = { repository.getProductStockOutTasks() },
            onSuccess = { _tasks.value = it },
        )
    }
}
