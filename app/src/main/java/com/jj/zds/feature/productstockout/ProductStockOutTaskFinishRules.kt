package com.jj.zds.feature.productstockout

import com.jj.zds.data.dto.productstockout.ProductStockOutChildTaskDto

object ProductStockOutTaskFinishRules {

    fun canFinish(childTasks: List<ProductStockOutChildTaskDto>): Boolean {
        return childTasks.isNotEmpty() && childTasks.all {
            it.status == STATUS_FINISHED || it.statusLabel == STATUS_FINISHED_LABEL
        }
    }

    private const val STATUS_FINISHED = 2
    private const val STATUS_FINISHED_LABEL = "已完成"
}
