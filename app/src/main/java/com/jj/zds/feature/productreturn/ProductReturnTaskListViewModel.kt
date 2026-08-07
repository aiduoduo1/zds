package com.jj.zds.feature.productreturn

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jj.arch.state.JiangLoadingMode
import com.jj.arch.vm.JiangViewModel
import com.jj.zds.data.dto.productreturn.AirportDto
import com.jj.zds.data.dto.productreturn.ProductReturnTaskDto
import com.jj.zds.data.repository.ZdsRepository

class ProductReturnTaskListViewModel : JiangViewModel() {
    private val repository by lazy { ZdsRepository() }
    private val _tasks = MutableLiveData<List<ProductReturnTaskDto>>(emptyList())
    val tasks: LiveData<List<ProductReturnTaskDto>> = _tasks
    private val _airports = MutableLiveData<List<AirportDto>>(emptyList())
    val airports: LiveData<List<AirportDto>> = _airports
    private val _taskCreated = MutableLiveData<Unit>()
    val taskCreated: LiveData<Unit> = _taskCreated

    fun loadTasks() = launchResult(
        loadingMode = JiangLoadingMode.PAGE,
        block = { repository.getProductReturnTasks() },
        onSuccess = { _tasks.value = it },
    )

    fun loadAirports() = launchResult(
        loadingMode = JiangLoadingMode.DIALOG,
        block = { repository.getReturnAirports() },
        onSuccess = { _airports.value = it },
    )

    fun createTask(airId: String, projectName: String) = launchResult(
        loadingMode = JiangLoadingMode.DIALOG,
        block = { repository.createProductReturnTask(airId, projectName) },
        onSuccess = { _taskCreated.value = Unit },
    )
}
