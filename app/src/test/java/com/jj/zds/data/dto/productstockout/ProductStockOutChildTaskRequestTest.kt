package com.jj.zds.data.dto.productstockout

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductStockOutChildTaskRequestTest {

    @Test
    fun `child task request keeps the selected delivery task id`() {
        assertEquals("delivery-task-id", ProductStockOutChildTaskRequest("delivery-task-id").taskId)
    }

    @Test
    fun `delivery detail request keeps the selected child task id as dtlId`() {
        assertEquals("delivery-detail-id", ProductStockOutDetailRequest("delivery-detail-id").dtlId)
    }

    @Test
    fun `delivery task keeps child tasks from the backend dtlList field`() {
        val childTask = ProductStockOutChildTaskDto(id = "child-task-id")
        val task = ProductStockOutTaskDto(dtlList = listOf(childTask))

        assertEquals(listOf(childTask), task.dtlList)
    }
}
