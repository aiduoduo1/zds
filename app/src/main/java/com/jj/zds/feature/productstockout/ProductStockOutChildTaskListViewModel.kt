package com.jj.zds.feature.productstockout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.zds.data.dto.productstockout.ProductStockOutChildTaskDto
import com.jj.zds.data.repository.ZdsRepository

class ProductStockOutChildTaskListViewModel : JiangViewModel() {

    private val repository by lazy { ZdsRepository() }

    private val _childTasks = MutableLiveData<List<ProductStockOutChildTaskDto>>(emptyList())
    val childTasks: LiveData<List<ProductStockOutChildTaskDto>> = _childTasks

    fun loadChildTasks(taskId: String) {
        launchResult(
            loadingMode = JiangLoadingMode.PAGE,
            block = { repository.getProductStockOutChildTasks(taskId) },
            onSuccess = { _childTasks.value = it },
        )
    }

    fun finishTask(taskId: String) {
        launchResult(
            loadingMode = JiangLoadingMode.DIALOG,
            block = { repository.finishProductStockOutTask(taskId) },
            onSuccess = { _taskFinished.value = Unit },
        )
    }

    private val _taskFinished = MutableLiveData<Unit>()
    val taskFinished: LiveData<Unit> = _taskFinished
}
